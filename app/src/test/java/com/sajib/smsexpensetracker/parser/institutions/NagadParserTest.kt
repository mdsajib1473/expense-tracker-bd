package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Body strings follow the real Nagad wording from sender "NAGAD" in a
 * third-party export, but every phone number, TxnID, OTP and amount is
 * invented. Fields are separated by literal newlines, as in the real format.
 */
class NagadParserTest {

    private val parser = NagadParser()

    @Test
    fun `cash in is parsed as credit with agent as counterparty`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("5000.00"), result?.amount)
        assertEquals(BigDecimal("5032.90"), result?.balance)
        assertEquals("01700000001", result?.counterparty)
        assertEquals("72AAAAAA", result?.reference)
    }

    @Test
    fun `cash in with promo prefix and short labels is parsed`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "WIN LAND IN DHAKA! CLICK NOW  nagad.io/xyz\nCash In Received.\nAmt:Tk 200.00\nUddokta:01700000002\nTxnID: 72BBBBBB\nBal:208.65\n27/03/2024 15:54",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("200.00"), result?.amount)
        assertEquals(BigDecimal("208.65"), result?.balance)
        assertEquals("01700000002", result?.counterparty)
        assertEquals("72BBBBBB", result?.reference)
    }

    @Test
    fun `money received is parsed as credit with sender phone as counterparty`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Money Received.\nAmount: Tk 20300.00\nSender: 01700000003\nRef: N/A\nTxnID: 73CCCCCC\nBalance: Tk 20300.32\n23/01/2025 20:58",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("20300.00"), result?.amount)
        assertEquals(BigDecimal("20300.32"), result?.balance)
        assertEquals("01700000003", result?.counterparty)
        assertEquals("73CCCCCC", result?.reference)
    }

    @Test
    fun `payment is parsed as debit with merchant as counterparty`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk 32.90\n03/11/2023 13:58",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("107.07"), result?.amount)
        assertEquals(BigDecimal("32.90"), result?.balance)
        assertEquals("Sample Ministry", result?.counterparty)
        assertEquals("72DDDDDD", result?.reference)
    }

    @Test
    fun `ecom otp message is never parsed`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Your One Time Password (OTP) for Nagad ECOM is 000000. Validity for OTP is 10 minutes. Helpline 16167.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `device registration otp message is never parsed`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Never Share Any Code.\nDevice registration request.\nUsername:01700000009\nOTP/Code: 000000\nExpiry time:09:51:32\nUID: AAAAAAAAAAA",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `bangla fraud warning is never parsed`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "সতর্কতা: আপনার পিন বা কোড কাউকে দেবেন না।",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `recharge cashback notice is never parsed`() {
        val result = parser.parse(
            sender = "NAGAD",
            body = "Congrats! You've received Cashback 5.0 Tk for Mobile Recharge of 100.0 Tk. | 01/02/2024 10:00 | For Details Call 16167",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns contain only the verified address`() {
        assertEquals(listOf("NAGAD"), parser.senderPatterns)
    }
}
