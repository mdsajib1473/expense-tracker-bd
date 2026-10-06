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
    fun `sender patterns contain only the verified address`() {
        assertEquals(listOf("UTTARA BANK"), parser.senderPatterns)
    }
}
