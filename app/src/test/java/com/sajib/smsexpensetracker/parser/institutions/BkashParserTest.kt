package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Every body string here is a real bKash SMS, taken from the project's own
 * transaction history, not a guessed format. Amounts with commas are
 * deliberately included since that is a confirmed real edge case.
 */
class BkashParserTest {

    private val parser = BkashParser()

    @Test
    fun `send money success is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Send Money Tk 510.00 to 01813738081 successful. Ref Sajib. Fee Tk 5.00. Balance Tk 4,479.65. TrxID DCK1B9P81H at 20/03/2026 00:08",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("510.00"), result?.amount)
        assertEquals(BigDecimal("4479.65"), result?.balance)
        assertEquals("01813738081", result?.counterparty)
        assertEquals("DCK1B9P81H", result?.reference)
    }

    @Test
    fun `send money failure returns null, no false debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Sorry, your Send Money request to 01765992095 is unsuccessful! Tk 30 has been returned. Balance Tk 73.50. TrxID BKA96AB813 at 10/11/2024 14:00",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `receive from phone number with comma amount is parsed as credit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 5,075.00 from 01735814791. Fee Tk 0.00. Balance Tk 6,075.50. TrxID BLU1H6X87P at 30/12/2024 13:22",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("5075.00"), result?.amount)
        assertEquals(BigDecimal("6075.50"), result?.balance)
        assertEquals("01735814791", result?.counterparty)
        assertEquals("BLU1H6X87P", result?.reference)
    }

    @Test
    fun `receive with ref and no space before Ref is still parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 20,172.00 from 01815474012.Ref jazakallah vai. Fee Tk 0.00. Balance Tk 20,329.00. TrxID CAK845MSGS at 20/01/2025 10:53",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("20172.00"), result?.amount)
        assertEquals("01815474012", result?.counterparty)
        assertEquals("CAK845MSGS", result?.reference)
    }

    @Test
    fun `receive with business name sender and no fee field is parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received Tk 63.00 from OnnorokomWeb. Balance Tk 1,082.65. TrxID DE971UR13F at 09/05/2026 21:32",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("63.00"), result?.amount)
        assertEquals("OnnorokomWeb", result?.counterparty)
        assertEquals("DE971UR13F", result?.reference)
    }

    @Test
    fun `bank deposit from Sonali Bank Account is parsed as credit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "You have received a deposit of Tk 1,000.00 from Sonali Bank Account. Fee Tk 0.00. Balance Tk 1,140.34. TrxID CGJ2RQENX4 at 19/07/2025 15:43",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("1000.00"), result?.amount)
        assertEquals("Sonali Bank Account", result?.counterparty)
        assertEquals("CGJ2RQENX4", result?.reference)
    }

    @Test
    fun `cash in from agent with trailing promo text is parsed correctly`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Cash In Tk 1,000.00 from 01835908735 successful. Fee Tk 0.00. Balance Tk 1,607.90. TrxID CK710WRNDB at 07/11/2025 22:21. Download App: https://bKa.sh/8app",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("1000.00"), result?.amount)
        assertEquals("01835908735", result?.counterparty)
        assertEquals("CK710WRNDB", result?.reference)
    }

    @Test
    fun `payment with 'Payment Tk' phrasing is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Payment Tk 100.00 to KAHF BANGLADESH LTD-RM67912 is successful. Balance Tk 601.81. TrxID DG705TRZHM at 07/07/2026 20:36",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("100.00"), result?.amount)
        assertEquals("KAHF BANGLADESH LTD-RM67912", result?.counterparty)
        assertEquals("DG705TRZHM", result?.reference)
    }

    @Test
    fun `payment with 'Payment of Tk' phrasing is parsed as debit`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Payment of Tk 1,015.50 to AS-SUNNAH FOUNDATION-RM56187 is successful. Balance Tk 1,115.15. TrxID DCD92UO50N at 13/03/2026 15:30",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("1015.50"), result?.amount)
        assertEquals("AS-SUNNAH FOUNDATION-RM56187", result?.counterparty)
        assertEquals("DCD92UO50N", result?.reference)
    }

    @Test
    fun `otp message is never parsed as a transaction`() {
        val result = parser.parse(
            sender = "bKash",
            body = "Your bKash verification code is 956586. Expires in 2 minutes.",
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