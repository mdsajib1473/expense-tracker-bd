package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionLookup
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Covers the share and paste intake use cases with the real parsers and
 * fake storage. Every body string is anonymized; every value is invented.
 */
class ManualEntryUseCasesTest {

    /** In-memory store with the repository's duplicate rule and a similar-transaction check. */
    private class FakeStore : TransactionWriter, TransactionLookup {
        val saved = mutableListOf<Pair<ParsedTransaction, Long>>()

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

        override suspend fun hasSimilar(
            institutionName: String,
            type: TransactionType,
            amount: BigDecimal,
            balance: BigDecimal?
        ): Boolean = saved.any { (existing, _) ->
            existing.institutionName == institutionName &&
                existing.result.type == type &&
                existing.result.amount == amount &&
                existing.result.balance == balance
        }
    }

    /** Counts calls, so a test can prove a text never reached the parsers. */
    private class CountingParser : SmsParser {
        var calls = 0
        override val institutionName = "Sample Bank"
        override val senderPatterns = listOf("SAMPLEBANK")
        override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
            calls++
            return null
        }
    }

    private val store = FakeStore()
    private val prepare = PrepareManualEntryUseCase(ParserEngine(ParserTestMessages.allParsers()), store)
    private val save = SaveManualEntryUseCase(store)

    private val bkashSendMoney =
        "Send Money Tk 500.00 to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"
    private val otp = "864209"
    private val otpBody = "Your bKash verification code is $otp. Expires in 2 minutes."

    @Test
    fun `recognized message gives a preview of the parsed fields only`() = runTest {
        val result = prepare(bkashSendMoney)

        assertEquals(
            ManualEntryPreparation.Preview(
                institutionName = "bKash",
                type = TransactionType.DEBIT,
                amount = BigDecimal("500.00"),
                balance = BigDecimal("4495.00"),
                reference = "AAAAAAAAAA",
                similarExists = false
            ),
            result
        )
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `saving a preview stores it at the confirmed time with no message text and no counterparty`() = runTest {
        val preview = prepare(bkashSendMoney) as ManualEntryPreparation.Preview

        val result = save(preview, receivedAt = 1_700_000_000_000L)

        assertEquals(ManualEntrySaveResult.Saved, result)
        val (parsed, receivedAt) = store.saved.single()
        assertEquals(1_700_000_000_000L, receivedAt)
        assertEquals("bKash", parsed.institutionName)
        assertEquals(BigDecimal("500.00"), parsed.result.amount)
        assertEquals(BigDecimal("4495.00"), parsed.result.balance)
        assertEquals("AAAAAAAAAA", parsed.result.reference)
        assertEquals(SaveManualEntryUseCase.NO_MESSAGE_TEXT, parsed.result.rawSms)
        assertEquals(null, parsed.result.counterparty)
    }

    @Test
    fun `saving the same preview at the same time again is a duplicate`() = runTest {
        val preview = prepare(bkashSendMoney) as ManualEntryPreparation.Preview
        save(preview, receivedAt = 1_000L)

        assertEquals(ManualEntrySaveResult.Duplicate, save(preview, receivedAt = 1_000L))
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `a stored transaction with the same amount and balance at another time raises the similar warning`() = runTest {
        save(prepare(bkashSendMoney) as ManualEntryPreparation.Preview, receivedAt = 1_000L)

        val again = prepare(bkashSendMoney) as ManualEntryPreparation.Preview

        assertTrue(again.similarExists)
        assertEquals(ManualEntrySaveResult.Saved, save(again, receivedAt = 2_000L))
        assertEquals(2, store.saved.size)
    }

    @Test
    fun `text longer than the limit is rejected before parsing`() = runTest {
        val parser = CountingParser()
        val prepare = PrepareManualEntryUseCase(ParserEngine(listOf(parser)), store)

        val result = prepare(bkashSendMoney + " ".repeat(PrepareManualEntryUseCase.MAX_TEXT_LENGTH))

        assertEquals(ManualEntryPreparation.TooLong, result)
        assertEquals(0, parser.calls)
    }

    @Test
    fun `text of exactly the limit is still parsed`() = runTest {
        val parser = CountingParser()
        val prepare = PrepareManualEntryUseCase(ParserEngine(listOf(parser)), store)

        val result = prepare("x".repeat(PrepareManualEntryUseCase.MAX_TEXT_LENGTH))

        assertEquals(ManualEntryPreparation.NoMatch, result)
        assertEquals(1, parser.calls)
    }

    @Test
    fun `blank text is rejected before parsing`() = runTest {
        val parser = CountingParser()
        val prepare = PrepareManualEntryUseCase(ParserEngine(listOf(parser)), store)

        assertEquals(ManualEntryPreparation.Blank, prepare(""))
        assertEquals(ManualEntryPreparation.Blank, prepare("  \n\t "))
        assertEquals(0, parser.calls)
    }

    @Test
    fun `unrecognized otp text is no match and nothing is stored`() = runTest {
        assertEquals(ManualEntryPreparation.NoMatch, prepare(otpBody))
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `body that two providers accept is ambiguous`() = runTest {
        val bkash = ParserTestMessages.allParsers().first { it.institutionName == "bKash" }
        val lookalike = object : SmsParser by bkash {
            override val institutionName = "Sample Wallet"
            override val senderPatterns = listOf("SAMPLEWALLET")
        }
        val prepare = PrepareManualEntryUseCase(ParserEngine(listOf(bkash, lookalike)), store)

        assertEquals(ManualEntryPreparation.Ambiguous, prepare(bkashSendMoney))
    }

    @Test
    fun `no result prints the input text`() = runTest {
        val longText = otpBody + "x".repeat(PrepareManualEntryUseCase.MAX_TEXT_LENGTH)
        val inputs = listOf(bkashSendMoney, otpBody, longText, " ")

        inputs.forEach { input ->
            val printed = prepare(input).toString()
            assertFalse(printed, printed.contains(input))
            assertFalse(printed, printed.contains(otp))
        }
        val preview = prepare(bkashSendMoney) as ManualEntryPreparation.Preview
        assertFalse(preview.toString().contains("01700000004"))
        assertFalse(save(preview, receivedAt = 1L).toString().contains(bkashSendMoney))
        assertFalse(save(preview, receivedAt = 1L).toString().contains("01700000004"))
    }
}
