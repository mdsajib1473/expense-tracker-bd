package com.sajib.smsexpensetracker.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.db.AppDatabase
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.institutions.CraftedAmountParserTest
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
 * Imports synthetic backup files that hold crafted messages into a real
 * in-memory Room database through the real repository: the import finishes,
 * counts each crafted message as unrecognized, and neither stores nor logs
 * its body. Every phone number, TxnID, OTP and amount is invented.
 */
@RunWith(RobolectricTestRunner::class)
class ImportSmsBackupCraftedMessageRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository

    private val parsers = ParserTestMessages.allParsers()
    private val crafted = CraftedAmountParserTest.cases()
    private val validBkash = ParserTestMessages.transactions.first { it.institutionName == "bKash" }
    private val validNagad = ParserTestMessages.transactions.first { it.institutionName == "Nagad" }

    @Before
    fun setUp() {
        ShadowLog.clear()
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun importer(engineParsers: List<SmsParser> = parsers) = ImportSmsBackupUseCase(
        SmsBackupReader { KXmlParser() },
        IngestSmsUseCase(ParserEngine(engineParsers), repository)
    )

    private fun senderOf(institutionName: String): String =
        parsers.single { it.institutionName == institutionName }.senderPatterns.first()

    @Test
    fun `crafted messages are counted as unrecognized and the valid ones around them are saved`() = runTest {
        val craftedElements = crafted.mapIndexed { index, (institutionName, _, body) ->
            sms(address = senderOf(institutionName), body = body, date = 10_000L + index)
        }
        val file = document(
            sms(address = validBkash.sender, body = validBkash.body, date = 1_000L),
            *craftedElements.toTypedArray(),
            sms(address = validNagad.sender, body = validNagad.body, date = 2_000L)
        )

        val summary = importer()(file.byteInputStream())

        assertEquals(
            ImportSummary(
                totalRead = crafted.size + 2,
                saved = 2,
                duplicates = 0,
                unrecognized = crafted.size,
                skippedNotInbox = 0,
                invalid = 0
            ),
            summary
        )
        val stored = repository.getAll().first()
        assertEquals(listOf(validNagad.body, validBkash.body), stored.map { it.rawSms })
    }

    @Test
    fun `no crafted body is stored or logged`() = runTest {
        val marker = "CRAFTEDMARKER"
        val body = "Send Money Tk , to 01700000004 successful. Ref $marker. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"

        val summary = importer()(document(sms(address = "bKash", body = body)).byteInputStream())

        assertEquals(1, summary.unrecognized)
        assertEquals(0, repository.getAll().first().size)
        assertFalse(summary.toString().contains(marker))
        assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(marker) })
    }

    @Test
    fun `a parser that throws does not stop the import and its exception text is not logged`() = runTest {
        val otp = "917305"
        val failing = object : SmsParser {
            override val institutionName = "Sample Bank"
            override val senderPatterns = listOf("SAMPLEBANK")
            override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? =
                throw IllegalStateException("failed on: $body")
        }
        val file = document(
            sms(address = "SAMPLEBANK", body = "Your verification code is $otp", date = 1_000L),
            sms(address = validBkash.sender, body = validBkash.body, date = 2_000L)
        )

        val summary = importer(parsers + failing)(file.byteInputStream())

        assertEquals(
            ImportSummary(totalRead = 2, saved = 1, duplicates = 0, unrecognized = 1, skippedNotInbox = 0, invalid = 0),
            summary
        )
        assertFalse(repository.getAll().first().any { it.rawSms.contains(otp) })
        assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(otp) || it.throwable?.message.orEmpty().contains(otp) })
    }
}
