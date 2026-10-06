package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Body strings follow the real Pubali Bank wording from sender "PUBALI BANK",
 * but every account mask, amount and branch or bank name is invented.
 */
class PubaliBankParserTest {

    private val parser = PubaliBankParser()

    @Test
    fun `branch credit with spaced mask and Dr balance is parsed with negative balance`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 140000 has been credited to your account *** 865 through SAMPLE BAZAR, CITY branch on 20-08-2023. A/C Bal: Dr 334050.05",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("140000"), result?.amount)
        assertEquals(BigDecimal("-334050.05"), result?.balance)
        assertEquals("SAMPLE BAZAR, CITY", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch debit with Dr balance is parsed as debit with negative balance`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 50000 has been debited from your account ***865 through SAMPLE BAZAR branch on 24-08-2023. A/C Bal: Dr 434061.55",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("50000"), result?.amount)
        assertEquals(BigDecimal("-434061.55"), result?.balance)
        assertEquals("SAMPLE BAZAR", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `eft credit with minus balance keeps balance negative`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 4374.78 has been credited to your a/c ***2865 through EFT SAMPLE BANK LIMITE,REMITTANCE,SAMPLEBANKLTD on 14-NOV-23. A/C Bal:-283050.77",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("4374.78"), result?.amount)
        assertEquals(BigDecimal("-283050.77"), result?.balance)
        assertEquals("EFT SAMPLE BANK LIMITE,REMITTANCE,SAMPLEBANKLTD", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `fund transfer credit with trailing period is parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 70000.00 has been credited to your account using FUND TRANSFER from SAMPLE COMMERCIAL BANK LIMITED on 24-DEC-23. A/C Bal:Dr 330365.72.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("70000.00"), result?.amount)
        assertEquals(BigDecimal("-330365.72"), result?.balance)
        assertEquals("SAMPLE COMMERCIAL BANK LIMITED", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `cheque book collection notice is never parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Please collect your Cheque Book (0000-000-00000) from SAMPLE Branch",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns contain only the verified address`() {
        assertEquals(listOf("PUBALI BANK"), parser.senderPatterns)
    }
}
