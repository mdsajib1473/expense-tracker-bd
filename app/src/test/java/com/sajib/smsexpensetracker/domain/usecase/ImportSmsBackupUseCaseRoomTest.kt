package com.sajib.smsexpensetracker.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.db.AppDatabase
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import com.sajib.smsexpensetracker.parser.institutions.NagadParser
import com.sajib.smsexpensetracker.parser.institutions.UttaraBankParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kxml2.io.KXmlParser
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Imports synthetic backup files into a real in-memory Room database through
 * the real repository, so the duplicate check is the production one. Every
 * phone number, TxnID, OTP and amount is invented.
 */
@RunWith(RobolectricTestRunner::class)
class ImportSmsBackupUseCaseRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var importBackup: ImportSmsBackupUseCase

    private val otp = "246810"
    private val file = document(
        sms(address = "bKash", body = "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00", date = 1_000L),
        sms(address = "UTTARA BANK", body = "Dear Customer, BDT 14000  has been credited to your A/C No 032300000000003 from Head Office and Current Balance is: BDT 52000.50", date = 2_000L),
        sms(address = "NAGAD", body = "Money Received.\nAmount: Tk 20300.00\nSender: 01700000003\nRef: N/A\nTxnID: 73CCCCCC\nBalance: Tk 20300.32\n23/01/2025 20:58", date = 3_000L),
        sms(address = "bKash", body = "Your bKash verification code is $otp. Expires in 2 minutes.", date = 4_000L),
        sms(address = "bKash", body = "Send Money Tk 500.00 to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00", date = 5_000L, type = "2")
    )

    @Before
    fun setUp() {
        ShadowLog.clear()
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TransactionRepository(db.transactionDao())
        importBackup = ImportSmsBackupUseCase(
            SmsBackupReader { KXmlParser() },
            IngestSmsUseCase(ParserEngine(listOf(BkashParser(), NagadParser(), UttaraBankParser())), repository)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `importing the same file twice stores each transaction once`() = runTest {
        val first = importBackup(file.byteInputStream())
        val second = importBackup(file.byteInputStream())

        assertEquals(ImportSummary(totalRead = 5, saved = 3, duplicates = 0, unrecognized = 1, skippedNotInbox = 1, invalid = 0), first)
        assertEquals(ImportSummary(totalRead = 5, saved = 0, duplicates = 3, unrecognized = 1, skippedNotInbox = 1, invalid = 0), second)
        val stored = repository.getAll().first()
        assertEquals(listOf("Nagad", "Uttara Bank", "bKash"), stored.map { it.institutionName })
        assertEquals(listOf(3_000L, 2_000L, 1_000L), stored.map { it.receivedAt })
    }

    @Test
    fun `unrecognized otp body is neither stored nor logged`() = runTest {
        val summary = importBackup(file.byteInputStream())

        val stored = repository.getAll().first()
        assertFalse(stored.any { it.rawSms.contains(otp) })
        assertFalse(stored.any { it.rawSms.contains("verification") })
        assertFalse(summary.toString().contains(otp))
        assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(otp) || it.msg.orEmpty().contains("verification") })
    }

    @Test
    fun `rows saved before a damaged part stay saved and re-importing the whole file adds the rest`() = runTest {
        val damaged = file.substring(0, file.indexOf("NAGAD") + 2)

        val error = try {
            importBackup(damaged.byteInputStream())
            null
        } catch (e: BackupImportException) {
            e.error
        }
        assertEquals(BackupImportError.MALFORMED, error)
        assertEquals(2, repository.getAll().first().size)

        val repaired = importBackup(file.byteInputStream())

        assertEquals(ImportSummary(totalRead = 5, saved = 1, duplicates = 2, unrecognized = 1, skippedNotInbox = 1, invalid = 0), repaired)
        assertEquals(3, repository.getAll().first().size)
    }
}
