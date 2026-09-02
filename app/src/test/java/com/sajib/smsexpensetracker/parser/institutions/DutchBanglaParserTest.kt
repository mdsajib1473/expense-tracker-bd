package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Every body string here is a real DBBL SMS from sender "16216" on the
 * project's own device, not a guessed format. The cash-out body's missing
 * spaces between fields are the real format and must stay as they are.
 */
class DutchBanglaParserTest {

    private val parser = DutchBanglaParser()

    @Test
    fun `balance inquiry with comma amount is parsed with amount equal to balance`() {
        val result = parser.parse(
            sender = "16216",
            body = "Balance of your A/C:***0045 is BDT 45,755.13 as on 30/06/25. To download statement click https://app.dutchbanglabank.com/cbsstatement . For query call 16216",
            receivedAt = 0L
        )

        assertEquals(TransactionType.BALANCE_INQUIRY, result?.type)
        assertEquals(BigDecimal("45755.13"), result?.amount)
        assertEquals(BigDecimal("45755.13"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `balance inquiry without comma amount is parsed`() {
        val result = parser.parse(
            sender = "16216",
            body = "Balance of your A/C:***0045 is BDT 450.91 as on 31/12/24. To download statement click https://app.dutchbanglabank.com/cbsstatement . For query call 16216",
            receivedAt = 0L
        )

        assertEquals(TransactionType.BALANCE_INQUIRY, result?.type)
        assertEquals(BigDecimal("450.91"), result?.amount)
        assertEquals(BigDecimal("450.91"), result?.balance)
        assertNull(result?.reference)
    }

    @Test
    fun `atm to account transfer credit is parsed as credit with closing balance`() {
        val result = parser.parse(
            sender = "16216",
            body = "Dear Sir, your A/C ***0045 credited (ATM A/C to A/C Transfer Credit) by Tk50,000.00 on 01-02-2025 02:55:44 PM C/B Tk50,459.01. NexusPay https://bit.ly/nexuspay",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("50000.00"), result?.amount)
        assertEquals(BigDecimal("50459.01"), result?.balance)
        assertEquals("ATM A/C to A/C Transfer Credit", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `nexuspay cash out with no spaces between fields is parsed as debit`() {
        val result = parser.parse(
            sender = "16216",
            body = "Cash-Out to A/C:***336Tk902.00Fee:Tk15.06 Your A/C Balance: Tk326.89.TxnId:5762726721Date:27-SEP-25 05:59:38 pm. Please download https://bit.ly/nexuspay",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("902.00"), result?.amount)
        assertEquals(BigDecimal("326.89"), result?.balance)
        assertEquals("***336", result?.counterparty)
        assertEquals("5762726721", result?.reference)
    }

    @Test
    fun `debit card otp message is never parsed as a transaction`() {
        val result = parser.parse(
            sender = "16216",
            body = "Dear Customer, 671016 is your OTP for e-com transaction of Debit Card no# 4840****7242 which will be valid for 5 minutes.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `unrecognized DBBL message returns null instead of crashing`() {
        val result = parser.parse(
            sender = "16216",
            body = "Some future DBBL message format we have never seen before.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns cover the confirmed short code and the alphanumeric id`() {
        assertEquals(listOf("16216", "DUTCHB"), parser.senderPatterns)
    }
}
