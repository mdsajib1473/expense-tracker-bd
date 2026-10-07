package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Every body string here follows the wording and layout of a real bKash SMS,
 * but every phone number, name, TrxID, OTP, amount and balance is invented.
 * Amounts with commas are deliberately included since that is a confirmed
 * real edge case.
 */
class BkashParserTest {

    private val parser = BkashParser()

    @Test
    fun `send money success is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Send Money Tk 500.00 to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("500.00"), result?.amount)
        assertEquals(BigDecimal("4495.00"), result?.balance)
        assertEquals("01700000004", result?.counterparty)
        assertEquals("AAAAAAAAAA", result?.reference)
    }

    @Test
    fun `send money failure returns null, no false debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Sorry, your Send Money request to 01700000005 is unsuccessful! Tk 30 has been returned. Balance Tk 70.00. TrxID BBBBBBBBBB at 01/11/2024 10:00",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `receive from phone number with comma amount is parsed as credit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("5000.00"), result?.amount)
        assertEquals(BigDecimal("6000.00"), result?.balance)
        assertEquals("01700000006", result?.counterparty)
        assertEquals("CCCCCCCCCC", result?.reference)
    }

    @Test
    fun `receive with ref and no space before Ref is still parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 20,000.00 from 01700000007.Ref sample note. Fee Tk 0.00. Balance Tk 20,150.00. TrxID DDDDDDDDDD at 01/01/2025 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("20000.00"), result?.amount)
        assertEquals("01700000007", result?.counterparty)
        assertEquals("DDDDDDDDDD", result?.reference)
    }

    @Test
    fun `receive with business name sender and no fee field is parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 60.00 from SAMPLE MERCHANT. Balance Tk 1,060.00. TrxID EEEEEEEEEE at 01/05/2026 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("60.00"), result?.amount)
        assertEquals("SAMPLE MERCHANT", result?.counterparty)
        assertEquals("EEEEEEEEEE", result?.reference)
    }

    @Test
    fun `bank deposit from Sonali Bank Account is parsed as credit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received a deposit of Tk 1,000.00 from Sonali Bank Account. Fee Tk 0.00. Balance Tk 1,100.00. TrxID FFFFFFFFFF at 01/07/2025 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("1000.00"), result?.amount)
        assertEquals("Sonali Bank Account", result?.counterparty)
        assertEquals("FFFFFFFFFF", result?.reference)
    }

    @Test
    fun `cash in from agent with trailing promo text is parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Cash In Tk 1,000.00 from 01700000008 successful. Fee Tk 0.00. Balance Tk 1,500.00. TrxID GGGGGGGGGG at 01/11/2025 10:00. Download App: https://bKa.sh/8app",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("1000.00"), result?.amount)
        assertEquals("01700000008", result?.counterparty)
        assertEquals("GGGGGGGGGG", result?.reference)
    }

    @Test
    fun `payment with 'Payment Tk' phrasing is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("100.00"), result?.amount)
        assertEquals("SAMPLE MERCHANT-RM00001", result?.counterparty)
        assertEquals("HHHHHHHHHH", result?.reference)
    }

    @Test
    fun `payment with 'Payment of Tk' phrasing is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Payment of Tk 1,000.50 to SAMPLE MERCHANT-RM00002 is successful. Balance Tk 1,100.00. TrxID JJJJJJJJJJ at 01/03/2026 10:00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("1000.50"), result?.amount)
        assertEquals("SAMPLE MERCHANT-RM00002", result?.counterparty)
        assertEquals("JJJJJJJJJJ", result?.reference)
    }

    @Test
    fun `otp message is never parsed as a transaction`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Your bKash verification code is 000000. Expires in 2 minutes.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `unrecognized bKash message returns null instead of crashing`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Some future bKash message format we have never seen before.",
            receivedAt = 0L
        )

        assertNull(result)
    }
}