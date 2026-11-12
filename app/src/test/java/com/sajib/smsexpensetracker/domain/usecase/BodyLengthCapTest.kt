package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.domain.model.IngestResult
import com.sajib.smsexpensetracker.domain.model.SmsLimits
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser

/**
 * Covers the body length cap applied before parsing: a body up to the cap
 * is parsed as before, a longer one is unrecognized without reaching any
 * parser or the store, and a very long crafted body is rejected at once
 * instead of costing seconds of pattern matching. Every value is invented.
 */
class BodyLengthCapTest {

    private class FakeTransactionWriter : TransactionWriter {
        val saved = mutableListOf<ParsedTransaction>()

        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            saved += parsed
            return true
        }
    }

    /** Fails the test if the engine ever hands it a body. */
    private class MustNotBeCalledParser : SmsParser {
        override val institutionName = "Sample Bank"
        override val senderPatterns = listOf("SAMPLEBANK")
        override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? =
            throw AssertionError("parser was called with a body of ${body.length} characters")
    }

    private val writer = FakeTransactionWriter()
    private val ingest = IngestSmsUseCase(ParserEngine(ParserTestMessages.allParsers()), writer)
    private val valid = ParserTestMessages.transactions.first { it.institutionName == "bKash" }

    /** The shape that costs the bKash parser seconds when it is parsed at this length. */
    private val hugeCraftedBody = "Payment Tk 1 to ".repeat(HUGE_LENGTH / 16)

    /** A valid bKash message followed by filler, [length] characters in total. */
    private fun validBodyOfLength(length: Int): String =
        valid.body + " " + "x".repeat(length - valid.body.length - 1)

    private inline fun elapsedMillis(block: () -> Unit): Long {
        val startedAt = System.nanoTime()
        block()
        return (System.nanoTime() - startedAt) / NANOS_PER_MILLI
    }

    @Test
    fun `body just under the cap and exactly at the cap is parsed and saved as before`() = runTest {
        listOf(SmsLimits.MAX_BODY_LENGTH - 1, SmsLimits.MAX_BODY_LENGTH).forEachIndexed { index, length ->
            val body = validBodyOfLength(length)

            assertEquals(length, body.length)
            assertEquals(IngestResult.Saved("bKash"), ingest(valid.sender, body, receivedAt = 1_000L + index))
        }
        assertEquals(2, writer.saved.size)
    }

    @Test
    fun `body one character over the cap is unrecognized and not stored, though its text is a valid message`() = runTest {
        val body = validBodyOfLength(SmsLimits.MAX_BODY_LENGTH + 1)

        val result = ingest(valid.sender, body, receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `body over the cap never reaches a parser`() = runTest {
        val guarded = IngestSmsUseCase(ParserEngine(listOf(MustNotBeCalledParser())), writer)

        val result = guarded("SAMPLEBANK", "x".repeat(SmsLimits.MAX_BODY_LENGTH + 1), receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `result for a body over the cap never holds or prints the body`() = runTest {
        val marker = "SECRETMARKER"
        val body = marker + "x".repeat(SmsLimits.MAX_BODY_LENGTH)

        val result = ingest(valid.sender, body, receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertFalse(result.toString().contains(marker))
        assertFalse(result.toString().contains("xxx"))
    }

    @Test
    fun `very long crafted body is rejected through ingest in well under a second`() = runTest {
        assertEquals(HUGE_LENGTH, hugeCraftedBody.length)
        var result: IngestResult? = null

        val millis = elapsedMillis { result = ingest("bKash", hugeCraftedBody, receivedAt = 1_000L) }

        assertEquals(IngestResult.Unrecognized, result)
        assertTrue("took $millis ms", millis < MAX_REJECTION_MILLIS)
    }

    @Test
    fun `very long crafted body is counted as unrecognized by the backup import in well under a second`() = runTest {
        val importBackup = ImportSmsBackupUseCase(SmsBackupReader { KXmlParser() }, ingest)
        val file = document(
            sms(address = "bKash", body = hugeCraftedBody, date = 1_000L),
            sms(address = valid.sender, body = valid.body, date = 2_000L)
        )
        var summary: ImportSummary? = null

        val millis = elapsedMillis { summary = importBackup(file.byteInputStream()) }

        assertEquals(
            ImportSummary(totalRead = 2, saved = 1, duplicates = 0, unrecognized = 1, skippedNotInbox = 0, invalid = 0),
            summary
        )
        assertEquals(listOf(valid.body), writer.saved.map { it.result.rawSms })
        assertTrue("took $millis ms", millis < MAX_REJECTION_MILLIS)
    }

    @Test
    fun `share and paste limit stays within the cap`() {
        assertTrue(PrepareManualEntryUseCase.MAX_TEXT_LENGTH <= SmsLimits.MAX_BODY_LENGTH)
        assertEquals(2000, PrepareManualEntryUseCase.MAX_TEXT_LENGTH)
        assertEquals(5000, SmsLimits.MAX_BODY_LENGTH)
    }

    private companion object {
        const val HUGE_LENGTH = 160_000
        const val MAX_REJECTION_MILLIS = 500L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
