package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.IngestResult
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.institutions.CraftedAmountParserTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the ingestion entry point, shared by the live receiver, the inbox
 * import and the backup import, against crafted messages: each is reported
 * as unrecognized, nothing is stored and nothing is thrown. Every value is
 * invented.
 */
class CraftedMessageIngestTest {

    private class FakeTransactionWriter : TransactionWriter {
        val saved = mutableListOf<ParsedTransaction>()

        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            saved += parsed
            return true
        }
    }

    private val writer = FakeTransactionWriter()
    private val parsers = ParserTestMessages.allParsers()
    private val ingest = IngestSmsUseCase(ParserEngine(parsers), writer)

    private fun senderOf(institutionName: String): String =
        parsers.single { it.institutionName == institutionName }.senderPatterns.first()

    @Test
    fun `every crafted body from its real sender is unrecognized and stores nothing`() = runTest {
        CraftedAmountParserTest.cases().forEach { (institutionName, case, body) ->
            val result = ingest(senderOf(institutionName), body, receivedAt = 1_000L)

            assertEquals("$institutionName: $case", IngestResult.Unrecognized, result)
        }
        assertTrue(writer.saved.isEmpty())
    }

    @Test
    fun `a valid message after a crafted one is still saved`() = runTest {
        val valid = ParserTestMessages.transactions.first { it.institutionName == "bKash" }
        val crafted = "Send Money Tk , to 01700000004 successful. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"

        val first = ingest(valid.sender, crafted, receivedAt = 1_000L)
        val second = ingest(valid.sender, valid.body, receivedAt = 2_000L)

        assertEquals(IngestResult.Unrecognized, first)
        assertEquals(IngestResult.Saved("bKash"), second)
        assertEquals(listOf(valid.body), writer.saved.map { it.result.rawSms })
    }

    @Test
    fun `a parser that throws is reported as unrecognized and its exception text goes nowhere`() = runTest {
        val body = "Your verification code is 739104"
        val failing = object : SmsParser {
            override val institutionName = "Sample Bank"
            override val senderPatterns = listOf("SAMPLEBANK")
            override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? =
                throw IllegalStateException("failed on: $body")
        }
        val ingest = IngestSmsUseCase(ParserEngine(listOf(failing)), writer)

        val result = ingest("SAMPLEBANK", body, receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertFalse(result.toString().contains("739104"))
        assertTrue(writer.saved.isEmpty())
    }
}
