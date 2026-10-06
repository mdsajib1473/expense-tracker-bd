package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Body strings follow the real Uttara Bank wording from sender "UTTARA BANK",
 * but every account number, amount, PIN and branch name is invented.
 */
class UttaraBankParserTest {

    private val parser = UttaraBankParser()

    @Test
    fun `credit with negative whole balance is parsed as credit`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 47800 has been credited to your A/C No 032300000000001 from Sample Road Branch and Current Balance is: BDT -1452200",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("47800"), result?.amount)
        assertEquals(BigDecimal("-1452200"), result?.balance)
        assertEquals("Sample Road Branch", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `debit with negative decimal balance is parsed as debit`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 200000 has been debited from your A/C No 032300000000002 from Sample Branch and Current Balance is: BDT -200258.75",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("200000"), result?.amount)
        assertEquals(BigDecimal("-200258.75"), result?.balance)
        assertEquals("Sample Branch", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `sms banking account created message with pin is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Valued Customer\nYour sms banking account has been created, a/c no 0323-00000000001 and pin no is 0000\nFor Help: Type ubl help and send to 26969.Thanks",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `loan instalment due reminder is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Your A/C:00000000001 Loan instalments will be due on 10-FEB-23. Please disregard if already paid. Thanks for banking with UTTARA BANK LTD",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `cheque book ready notice is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Sir, Your cheque book is ready for delivery, A/C No.: 0323-00000000002, Pls take delivery from Sample Branch.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `credit with double space and head office source is parsed with positive balance`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 14000  has been credited to your A/C No 032300000000003 from Head Office and Current Balance is: BDT 52000.50",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("14000"), result?.amount)
        assertEquals(BigDecimal("52000.50"), result?.balance)
        assertEquals("Head Office", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `credit from local office with negative balance is parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 1000 has been credited to your A/C No 032300000000003 from Local Office and Current Balance is: BDT -9000.00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("1000"), result?.amount)
        assertEquals(BigDecimal("-9000.00"), result?.balance)
        assertEquals("Local Office", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `sms service charge with missing spaces is parsed as plain debit`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 3000.50 has been debited from your A/C No 0323****0003 for SMS Service Charge  fromSample Road Branch and Current Balance is:BDT 48999.50",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("3000.50"), result?.amount)
        assertEquals(BigDecimal("48999.50"), result?.balance)
        assertEquals("Sample Road Branch", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `credit with masked account and extra spaces is parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 700.25 has been credited to your A/C No 0323****0003   from Sample Road Branch and Current Balance is:BDT 5000.10",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("700.25"), result?.amount)
        assertEquals(BigDecimal("5000.10"), result?.balance)
        assertEquals("Sample Road Branch", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `credit without balance is parsed with null balance`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, BDT 5000 has been credited to your A/C No 032300000000003 from Sample Road Branch",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("5000"), result?.amount)
        assertNull(result?.balance)
        assertEquals("Sample Road Branch", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `balance statement with decimals is parsed as balance inquiry`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. 12345.67 . For details, please contact your Branch.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.BALANCE_INQUIRY, result?.type)
        assertEquals(BigDecimal("12345.67"), result?.amount)
        assertEquals(BigDecimal("12345.67"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `balance statement with whole amount is parsed as balance inquiry`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. 12345 . For details, please contact your Branch.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.BALANCE_INQUIRY, result?.type)
        assertEquals(BigDecimal("12345"), result?.amount)
        assertEquals(BigDecimal("12345"), result?.balance)
        assertNull(result?.reference)
    }

    @Test
    fun `balance statement with negative amount is parsed as balance inquiry`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. -12345.67 . For Details, please contact your Branch.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.BALANCE_INQUIRY, result?.type)
        assertEquals(BigDecimal("-12345.67"), result?.amount)
        assertEquals(BigDecimal("-12345.67"), result?.balance)
        assertNull(result?.reference)
    }

    @Test
    fun `rtgs sent notice is never parsed to avoid double counting`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, your fund transfer through RTGS of TK. 100000 to PBL of A/C No. 000000000001 has been sent.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `contact center notice is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Dear Customer, For any banking queries and support, please call our Contact Center Number 16000. Thank you for banking with us.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `loan instalment due reminder with plc suffix is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "Your A/C:00000000001 Loan instalments will be due on 10-DEC-24. Please disregard if already paid. Thanks for banking with UTTARA BANK PLC.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `bangla security warning is never parsed`() {
        val result = parser.parse(
            sender = "UTTARA BANK",
            body = "অপরিচিত লিংকে ক্লিক করবেন না।",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns contain only the verified address`() {
        assertEquals(listOf("UTTARA BANK"), parser.senderPatterns)
    }
}
