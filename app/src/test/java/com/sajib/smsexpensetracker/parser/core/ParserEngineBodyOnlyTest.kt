package com.sajib.smsexpensetracker.parser.core

import com.sajib.smsexpensetracker.parser.ParserTestMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Covers identifying a message from its body alone, as shared or pasted
 * text arrives with no sender. Real parser bodies come from
 * [ParserTestMessages]; every value in them and below is invented.
 */
class ParserEngineBodyOnlyTest {

    private val engine = ParserEngine(ParserTestMessages.allParsers())

    /** Accepts any body containing [marker] and records the sender it was called with. */
    private class MarkerParser(
        override val institutionName: String,
        override val senderPatterns: List<String>,
        private val marker: String = "SAMPLE-MARKER"
    ) : SmsParser {
        val sendersSeen = mutableListOf<String>()

        override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
            sendersSeen += sender
            if (!body.contains(marker)) return null
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = BigDecimal("10.00"),
                balance = BigDecimal("20.00"),
                counterparty = null,
                reference = "REF0000",
                rawSms = body
            )
        }
    }

    @Test
    fun `every success template of every parser test is matched uniquely to its own institution`() {
        ParserTestMessages.transactions.forEach { sample ->
            val identification = engine.identifyByBody(sample.body, receivedAt = 0L)

            assertTrue(sample.body, identification is BodyIdentification.Matched)
            identification as BodyIdentification.Matched
            assertEquals(sample.body, sample.institutionName, identification.institutionName)
        }
    }

    @Test
    fun `body only parsing gives exactly the result of the sender routed parse`() {
        ParserTestMessages.transactions.forEach { sample ->
            val routed = engine.parse(sample.sender, sample.body, receivedAt = 0L)
            val bodyOnly = engine.identifyByBody(sample.body, receivedAt = 0L) as BodyIdentification.Matched

            assertEquals(sample.body, routed?.institutionName, bodyOnly.institutionName)
            assertEquals(sample.body, routed?.result, bodyOnly.result)
        }
    }

    @Test
    fun `every message a parser test rejects is no match by body too`() {
        ParserTestMessages.nonTransactions.forEach { sample ->
            assertEquals(sample.body, BodyIdentification.NoMatch, engine.identifyByBody(sample.body, receivedAt = 0L))
        }
    }

    @Test
    fun `otp texts are no match`() {
        listOf(
            "Your bKash verification code is 135790. Expires in 2 minutes.",
            "Your One Time Password (OTP) for Nagad ECOM is 135790. Validity for OTP is 10 minutes.",
            "Dear Customer, 135790 is your OTP for e-com transaction of Debit Card no# 0000****0000.",
            "Use OTP 135790 to proceed with Account Deposit. Please do not share it with anyone."
        ).forEach { otp ->
            assertEquals(otp, BodyIdentification.NoMatch, engine.identifyByBody(otp, receivedAt = 0L))
        }
    }

    @Test
    fun `unrelated, blank and Bangla promotional texts are no match`() {
        listOf(
            "See you at 7 tonight, bring the charger.",
            "",
            "   \n\t ",
            "বিশেষ অফার! আজই রিচার্জ করুন এবং ১০০ টাকায় ২ জিবি ইন্টারনেট নিন।"
        ).forEach { text ->
            assertEquals(BodyIdentification.NoMatch, engine.identifyByBody(text, receivedAt = 0L))
        }
    }

    @Test
    fun `two parsers that both accept the body give ambiguous`() {
        val engine = ParserEngine(
            listOf(MarkerParser("Sample Wallet A", listOf("WALLET-A")), MarkerParser("Sample Wallet B", listOf("WALLET-B")))
        )

        assertEquals(BodyIdentification.Ambiguous, engine.identifyByBody("Paid SAMPLE-MARKER", receivedAt = 0L))
    }

    @Test
    fun `one accepting parser among several gives matched with that institution`() {
        val engine = ParserEngine(
            listOf(
                MarkerParser("Sample Wallet A", listOf("WALLET-A"), marker = "ONLY-A"),
                MarkerParser("Sample Wallet B", listOf("WALLET-B"))
            )
        )

        val identification = engine.identifyByBody("Paid SAMPLE-MARKER", receivedAt = 0L)

        assertEquals("Sample Wallet B", (identification as BodyIdentification.Matched).institutionName)
    }

    @Test
    fun `each parser is called with the first of its own sender patterns`() {
        val first = MarkerParser("Sample Wallet A", listOf("WALLET-A", "16000"))
        val second = MarkerParser("Sample Wallet B", listOf("WALLET-B"))

        ParserEngine(listOf(first, second)).identifyByBody("anything", receivedAt = 0L)

        assertEquals(listOf("WALLET-A"), first.sendersSeen)
        assertEquals(listOf("WALLET-B"), second.sendersSeen)
    }

    @Test
    fun `a parser with no sender pattern is never called`() {
        val unroutable = MarkerParser("Sample Wallet A", emptyList())

        val identification = ParserEngine(listOf(unroutable)).identifyByBody("Paid SAMPLE-MARKER", receivedAt = 0L)

        assertEquals(BodyIdentification.NoMatch, identification)
        assertTrue(unroutable.sendersSeen.isEmpty())
    }

    @Test
    fun `a parser failing on a non numeric amount counts as not recognizing the body`() {
        val failing = object : SmsParser {
            override val institutionName = "Sample Bank"
            override val senderPatterns = listOf("SAMPLEBANK")
            override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? =
                throw NumberFormatException()
        }
        val engine = ParserEngine(listOf(failing, MarkerParser("Sample Wallet B", listOf("WALLET-B"))))

        assertEquals(BodyIdentification.NoMatch, engine.identifyByBody("no marker here", receivedAt = 0L))
        assertEquals(
            "Sample Wallet B",
            (engine.identifyByBody("Paid SAMPLE-MARKER", receivedAt = 0L) as BodyIdentification.Matched).institutionName
        )
    }

    @Test
    fun `matched result prints no message text`() {
        val body = ParserTestMessages.transactions.first().body

        val identification = engine.identifyByBody(body, receivedAt = 0L)

        assertFalse(identification.toString().contains(body))
        assertFalse(identification.toString().contains("TrxID"))
    }
}
