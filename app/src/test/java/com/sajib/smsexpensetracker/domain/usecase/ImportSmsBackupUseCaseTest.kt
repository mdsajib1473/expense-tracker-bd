package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.smsRaw
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.ImportProgress
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import com.sajib.smsexpensetracker.parser.institutions.NagadParser
import com.sajib.smsexpensetracker.parser.institutions.UttaraBankParser
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Covers importing a backup file through the real reader, a real
 * ParserEngine and a fake writer. Bodies reuse the templates of the parser
 * tests; every phone number, TxnID, OTP and amount is invented.
 */
class ImportSmsBackupUseCaseTest {

    /** In-memory writer with the repository's duplicate rule: institution, timestamp, amount, type and balance. */
    private class FakeTransactionWriter : TransactionWriter {
        val saved = CopyOnWriteArrayList<Pair<ParsedTransaction, Long>>()

        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            val duplicate = saved.any { (existing, at) ->
                existing.institutionName == parsed.institutionName &&
                    at == receivedAt &&
                    existing.result.amount == parsed.result.amount &&
                    existing.result.type == parsed.result.type &&
                    existing.result.balance == parsed.result.balance
            }
            if (duplicate) return false
            saved += parsed to receivedAt
            return true
        }
    }

    private val writer = FakeTransactionWriter()
    private val importBackup = ImportSmsBackupUseCase(
        SmsBackupReader { KXmlParser() },
        IngestSmsUseCase(ParserEngine(listOf(BkashParser(), NagadParser(), UttaraBankParser())), writer)
    )

    private val bkashReceived =
        "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00"
    private val uttaraCredit =
        "Dear Customer, BDT 14000  has been credited to your A/C No 032300000000003 from Head Office and Current Balance is: BDT 52000.50"
    private val nagadCashIn =
        "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"
    private val nagadPayment =
        "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk 32.90\n03/11/2023 13:58"
    private val banglaWarning = "সতর্কতা: আপনার পিন বা কোড কাউকে দেবেন না।"
    private val otp = "246810"
    private val otpBody = "Your bKash verification code is $otp. Expires in 2 minutes."

    private suspend fun import(xml: String, onProgress: (ImportProgress) -> Unit = {}): ImportSummary =
        importBackup(xml.byteInputStream(), onProgress)

    private fun summary(
        totalRead: Int,
        saved: Int = 0,
        duplicates: Int = 0,
        unrecognized: Int = 0,
        skippedNotInbox: Int = 0,
        invalid: Int = 0
    ) = ImportSummary(totalRead, saved, duplicates, unrecognized, skippedNotInbox, invalid)

    @Test
    fun `received bank message is recognized and saved with the backup timestamp`() = runTest {
        val result = import(document(sms(address = "UTTARA BANK", body = uttaraCredit, date = 1_733_047_200_000L)))

        assertEquals(summary(totalRead = 1, saved = 1), result)
        assertEquals("Uttara Bank", writer.saved.single().first.institutionName)
        assertEquals(BigDecimal("14000"), writer.saved.single().first.result.amount)
        assertEquals(1_733_047_200_000L, writer.saved.single().second)
    }

    @Test
    fun `sent message is skipped and never saved even when it looks like a transaction`() = runTest {
        val result = import(document(sms(address = "bKash", body = bkashReceived, type = "2")))

        assertEquals(summary(totalRead = 1, skippedNotInbox = 1), result)
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `bodies with an emoji pair or a lone surrogate do not fail the import`() = runTest {
        val xml = document(
            sms(address = "bKash", body = "$bkashReceived \uD83D\uDE00"),
            smsRaw("bKash", "Promo &#55357; offer &#xDE00; today", "1700000000001", "1"),
            sms(address = "UTTARA BANK", body = uttaraCredit, date = 1_700_000_000_002L)
        )

        val result = import(xml)

        assertEquals(summary(totalRead = 3, saved = 2, unrecognized = 1), result)
        assertEquals(listOf("bKash", "Uttara Bank"), writer.saved.map { it.first.institutionName })
    }

    @Test
    fun `newline references give a multi-line body that the Nagad parser recognizes`() = runTest {
        val xml = document(sms(address = "NAGAD", body = nagadCashIn))
        assertTrue(xml.contains("Cash In Received.&#10;Amount"))

        val result = import(xml)

        assertEquals(summary(totalRead = 1, saved = 1), result)
        val saved = writer.saved.single().first
        assertEquals("Nagad", saved.institutionName)
        assertEquals(nagadCashIn, saved.result.rawSms)
        assertEquals("01700000001", saved.result.counterparty)
    }

    @Test
    fun `apostrophe and quote references are decoded before parsing`() = runTest {
        val bkashQuotedMerchant =
            "Payment Tk 100.00 to \"SAMPLE MERCHANT\" is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"
        val xml = document(
            sms(address = "NAGAD", body = nagadPayment, date = 1L),
            sms(address = "bKash", body = bkashQuotedMerchant, date = 2L)
        )
        assertTrue(xml.contains("&apos;Sample Ministry&apos;") && xml.contains("&quot;SAMPLE MERCHANT&quot;"))

        val result = import(xml)

        assertEquals(summary(totalRead = 2, saved = 2), result)
        assertEquals("Sample Ministry", writer.saved[0].first.result.counterparty)
        assertEquals("\"SAMPLE MERCHANT\"", writer.saved[1].first.result.counterparty)
    }

    @Test
    fun `Bangla message is unrecognized and nothing is stored`() = runTest {
        val result = import(document(sms(address = "NAGAD", body = banglaWarning)))

        assertEquals(summary(totalRead = 1, unrecognized = 1), result)
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `same message twice in one file is saved once and reported once as a duplicate`() = runTest {
        val message = sms(address = "bKash", body = bkashReceived)

        val result = import(document(message, message))

        assertEquals(summary(totalRead = 2, saved = 1, duplicates = 1), result)
        assertEquals(1, writer.saved.size)
    }

    @Test
    fun `importing the same file twice saves nothing the second time`() = runTest {
        val xml = document(
            sms(address = "bKash", body = bkashReceived, date = 1L),
            sms(address = "UTTARA BANK", body = uttaraCredit, date = 2L),
            sms(address = "bKash", body = otpBody, date = 3L)
        )

        val first = import(xml)
        val second = import(xml)

        assertEquals(summary(totalRead = 3, saved = 2, unrecognized = 1), first)
        assertEquals(summary(totalRead = 3, duplicates = 2, unrecognized = 1), second)
        assertEquals(2, writer.saved.size)
    }

    @Test
    fun `otp message is unrecognized, persists nothing and leaks into no summary or error`() = runTest {
        val result = import(document(sms(address = "bKash", body = otpBody)))

        assertEquals(summary(totalRead = 1, unrecognized = 1), result)
        assertTrue(writer.saved.isEmpty())
        assertFalse(result.toString().contains(otp))
        assertFalse(result.toString().contains("verification"))

        val damaged = document(sms(address = "bKash", body = otpBody)).replace("Expires", "&Expires")
        val e = expectImportFailure { import(damaged) }
        assertFalse(e.message.orEmpty().contains(otp))
        assertFalse(e.toString().contains("verification"))
    }

    @Test
    fun `received messages missing attributes are counted as invalid and not parsed`() = runTest {
        val xml = document(
            smsRaw(address = null, body = SmsBackupXml.escape(bkashReceived), date = "1", type = "1"),
            smsRaw(address = "bKash", body = null, date = "1", type = "1"),
            smsRaw(address = "bKash", body = SmsBackupXml.escape(bkashReceived), date = null, type = "1"),
            smsRaw(address = "bKash", body = SmsBackupXml.escape(bkashReceived), date = "soon", type = "1")
        )

        val result = import(xml)

        assertEquals(summary(totalRead = 4, invalid = 4), result)
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `wrong root element fails with a typed error that holds no content`() = runTest {
        val xml = SmsBackupXml.PROLOG + "<calls><sms address=\"bKash\" body=\"$otpBody\" date=\"1\" type=\"1\" /></calls>"

        val e = expectImportFailure { import(xml) }

        assertEquals(BackupImportError.NOT_SMS_BACKUP, e.error)
        assertNull(e.cause)
        assertFalse(e.toString().contains(otp))
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `truncated file fails with a typed error and keeps the rows saved before the cut`() = runTest {
        val full = document(
            sms(address = "bKash", body = bkashReceived, date = 1L),
            sms(address = "UTTARA BANK", body = uttaraCredit, date = 2L),
            sms(address = "bKash", body = otpBody, date = 3L)
        )
        val truncated = full.substring(0, full.indexOf(otp) + 2)

        val e = expectImportFailure { import(truncated) }

        assertEquals(BackupImportError.MALFORMED, e.error)
        assertFalse(e.toString().contains("verification"))
        assertFalse(e.toString().contains("bKash"))
        assertEquals(2, writer.saved.size)
        assertEquals(summary(totalRead = 3, duplicates = 2, unrecognized = 1), import(full))
    }

    @Test
    fun `progress starts at zero with the count hint and ends at the processed total`() = runTest {
        val messages = Array(60) { i -> sms(address = "bKash", body = "Promo $i", date = 1L + i) }
        val reports = CopyOnWriteArrayList<ImportProgress>()

        import(document(*messages, count = 75)) { reports += it }

        assertEquals(ImportProgress(0, 75), reports.first())
        assertEquals(ImportProgress(60, 75), reports.last())
        assertEquals(listOf(0, 25, 50, 60), reports.map { it.processed })
    }

    @Test
    fun `large synthetic file streams through quickly with a bounded read-ahead`() = runTest {
        val count = 6_000
        val stream = SmsBackupXml.GeneratedBackup(count) { i -> syntheticMessage(i) }
        var maxReadAhead = 0

        val started = System.nanoTime()
        val result = importBackup(stream) { progress ->
            maxReadAhead = maxOf(maxReadAhead, stream.messagesGenerated - progress.processed)
        }
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000

        assertEquals(summary(totalRead = count, saved = 4_000, unrecognized = 1_000, skippedNotInbox = 1_000), result)
        assertTrue("took $elapsedMillis ms", elapsedMillis < 20_000)
        // Memory stays constant: the reader never pulls more than a few buffers ahead of the import.
        assertTrue("read $maxReadAhead messages ahead", maxReadAhead < 500)
    }

    @Test
    fun `cancelling stops the import and keeps the rows saved so far`() = runTest {
        val count = 10_000
        val stream = SmsBackupXml.GeneratedBackup(count) { i ->
            sms(address = "bKash", body = receivedTemplate(i), date = 1_700_000_000_000L + i)
        }
        lateinit var job: Job
        job = launch(start = CoroutineStart.LAZY) {
            importBackup(stream) { progress -> if (progress.processed >= 100) job.cancel() }
        }

        job.start()
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(writer.saved.size >= 100)
        assertTrue("saved ${writer.saved.size}", writer.saved.size < count)
    }

    private fun receivedTemplate(i: Int) =
        "You have received Tk ${i + 1}.00 from 01700000006. Fee Tk 0.00. Balance Tk ${i + 1_000}.00. TrxID T$i at 01/12/2024 10:00"

    /** Four received transactions, one unrecognized OTP-like message and one sent message in every six. */
    private fun syntheticMessage(i: Int): String {
        val date = 1_700_000_000_000L + i
        return when (i % 6) {
            4 -> sms(address = "bKash", body = "Your bKash verification code is 000000. Expires in 2 minutes.", date = date)
            5 -> sms(address = "bKash", body = receivedTemplate(i), date = date, type = "2")
            else -> sms(address = "bKash", body = receivedTemplate(i), date = date)
        }
    }

    private suspend fun expectImportFailure(block: suspend () -> Unit): BackupImportException =
        try {
            block()
            throw AssertionError("expected a BackupImportException")
        } catch (e: BackupImportException) {
            e
        }
}
