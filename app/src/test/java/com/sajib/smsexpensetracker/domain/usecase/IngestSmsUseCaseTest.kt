package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.IngestResult
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Covers the ingestion entry point with a real ParserEngine and a fake
 * writer. Every body string is anonymized; every value in it is invented.
 */
class IngestSmsUseCaseTest {

    /** In-memory writer that treats a repeated (institution, timestamp, amount) as a duplicate. */
    private class FakeTransactionWriter : TransactionWriter {
        val saved = mutableListOf<Pair<ParsedTransaction, Long>>()

        override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
            val duplicate = saved.any { (existing, at) ->
                existing.institutionName == parsed.institutionName &&
                    at == receivedAt &&
                    existing.result.amount == parsed.result.amount
            }
            if (duplicate) return false
            saved += parsed to receivedAt
            return true
        }
    }

    private val writer = FakeTransactionWriter()
    private val ingest = IngestSmsUseCase(ParserEngine(listOf(BkashParser())), writer)

    private val transactionBody =
        "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00"
    private val otpBody = "Your bKash verification code is 000000. Expires in 2 minutes."

    @Test
    fun `recognized new sms is saved and reports the institution`() = runTest {
        val result = ingest(sender = "bKash", body = transactionBody, receivedAt = 1_000L)

        assertEquals(IngestResult.Saved("bKash"), result)
        assertEquals(1, writer.saved.size)
        assertEquals(BigDecimal("5000.00"), writer.saved[0].first.result.amount)
        assertEquals(1_000L, writer.saved[0].second)
    }

    @Test
    fun `same sms ingested twice is reported as duplicate and stored once`() = runTest {
        ingest(sender = "bKash", body = transactionBody, receivedAt = 1_000L)
        val second = ingest(sender = "bKash", body = transactionBody, receivedAt = 1_000L)

        assertEquals(IngestResult.Duplicate("bKash"), second)
        assertEquals(1, writer.saved.size)
    }

    @Test
    fun `unrecognized sms persists nothing and the result holds no body`() = runTest {
        val result = ingest(sender = "bKash", body = otpBody, receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertTrue(writer.saved.isEmpty())
        assertFalse(result.toString().contains("000000"))
        assertFalse(result.toString().contains("verification"))
    }

    @Test
    fun `sms from a sender no parser handles is unrecognized`() = runTest {
        val result = ingest(sender = "UNKNOWN", body = transactionBody, receivedAt = 1_000L)

        assertEquals(IngestResult.Unrecognized, result)
        assertTrue(writer.saved.isEmpty())
    }
}
