package com.sajib.smsexpensetracker.ui.backupimport

import android.net.Uri
import com.sajib.smsexpensetracker.data.backup.BackupFileOpener
import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.domain.usecase.ImportSmsBackupUseCase
import com.sajib.smsexpensetracker.domain.usecase.IngestSmsUseCase
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kxml2.io.KXmlParser
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Covers the backup import state machine (Idle, Importing, Done, Failed,
 * and Stop back to Idle) with the real reader and use case and a fake file
 * opener. Robolectric provides android.util.Log. Every value is invented.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupImportViewModelTest {

    /** Opener whose behavior each test sets: hand over a stream, fail, or never finish. Closes the stream like the real one. */
    private class FakeOpener : BackupFileOpener {
        var open: suspend () -> InputStream = { error("not set") }
        val opened = CompletableDeferred<Unit>()
        var closedOrCancelled = false

        override suspend fun <T> read(uri: Uri, block: suspend (InputStream) -> T): T {
            opened.complete(Unit)
            try {
                return open().use { block(it) }
            } finally {
                closedOrCancelled = true
            }
        }
    }

    private class FakeWriter : TransactionWriter {
        val saved = CopyOnWriteArrayList<ParsedTransaction>()
        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            if (saved.any { it.result.reference == parsed.result.reference }) return false
            saved += parsed
            return true
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val opener = FakeOpener()
    private val writer = FakeWriter()
    private val uri = Uri.parse("content://sample.provider/backup.xml")
    private val otp = "642097"

    private val file = document(
        sms(address = "bKash", body = "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00", date = 1L),
        sms(address = "bKash", body = "Your bKash verification code is $otp. Expires in 2 minutes.", date = 2L),
        sms(address = "bKash", body = "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00", date = 3L, type = "2")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = BackupImportViewModel(
        opener,
        ImportSmsBackupUseCase(
            SmsBackupReader { KXmlParser() },
            IngestSmsUseCase(ParserEngine(listOf(BkashParser())), writer)
        )
    )

    private suspend fun BackupImportViewModel.awaitFinished(): BackupImportState =
        state.first { it !is BackupImportState.Importing }

    @Test
    fun `starts idle`() {
        assertEquals(BackupImportState.Idle, viewModel().state.value)
    }

    @Test
    fun `import goes from importing to done with counts only`() = runTest {
        opener.open = { file.byteInputStream() }
        val vm = viewModel()

        vm.import(uri)
        assertTrue(vm.state.value is BackupImportState.Importing)

        assertEquals(
            BackupImportState.Done(ImportSummary(totalRead = 3, saved = 1, duplicates = 0, unrecognized = 1, skippedNotInbox = 1, invalid = 0)),
            vm.awaitFinished()
        )
        assertEquals(1, writer.saved.size)
        assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(otp) })
        assertFalse(vm.state.value.toString().contains(otp))
    }

    @Test
    fun `a file that is not a backup ends in failed with its category`() = runTest {
        opener.open = { "<calls><call /></calls>".byteInputStream() }
        val vm = viewModel()

        vm.import(uri)

        assertEquals(BackupImportState.Failed(BackupImportError.NOT_SMS_BACKUP), vm.awaitFinished())
    }

    @Test
    fun `a file that cannot be opened ends in failed with read failed`() = runTest {
        opener.open = { throw BackupImportException(BackupImportError.READ_FAILED) }
        val vm = viewModel()

        vm.import(uri)

        assertEquals(BackupImportState.Failed(BackupImportError.READ_FAILED), vm.awaitFinished())
    }

    @Test
    fun `an IOException while closing the file ends in failed with read failed`() = runTest {
        opener.open = {
            object : InputStream() {
                private val bytes = file.byteInputStream()
                override fun read(): Int = bytes.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int = bytes.read(b, off, len)
                override fun close() = throw IOException("close failed")
            }
        }
        val vm = viewModel()

        vm.import(uri)

        assertEquals(BackupImportState.Failed(BackupImportError.READ_FAILED), vm.awaitFinished())
    }

    @Test
    fun `stop cancels the running import and returns to idle`() = runTest {
        opener.open = { awaitCancellation() }
        val vm = viewModel()

        vm.import(uri)
        opener.opened.await()
        vm.cancel()
        advanceUntilIdle()

        assertEquals(BackupImportState.Idle, vm.state.value)
        assertTrue(opener.closedOrCancelled)
    }

    @Test
    fun `a second import while one runs is ignored`() = runTest {
        opener.open = { awaitCancellation() }
        val vm = viewModel()
        vm.import(uri)
        opener.opened.await()
        val running = vm.state.value

        vm.import(Uri.parse("content://sample.provider/other.xml"))

        assertEquals(running, vm.state.value)
        vm.cancel()
    }

    @Test
    fun `dismiss clears a finished result but not a running import`() = runTest {
        opener.open = { file.byteInputStream() }
        val vm = viewModel()
        vm.import(uri)
        vm.awaitFinished()

        vm.dismiss()
        assertEquals(BackupImportState.Idle, vm.state.value)

        opener.open = { awaitCancellation() }
        val running = viewModel()
        running.import(uri)
        running.dismiss()
        assertTrue(running.state.value is BackupImportState.Importing)
        running.cancel()
    }

    @Test
    fun `importing the same file twice reports duplicates the second time`() = runTest {
        opener.open = { file.byteInputStream() }
        val vm = viewModel()
        vm.import(uri)
        vm.awaitFinished()
        vm.dismiss()

        vm.import(uri)

        val second = vm.awaitFinished() as BackupImportState.Done
        assertEquals(0, second.summary.saved)
        assertEquals(1, second.summary.duplicates)
    }
}
