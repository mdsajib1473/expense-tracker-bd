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
    fun `branch debit with Cr balance is parsed with positive balance`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 5000 has been debited from your account *** 123 through SAMPLE BAZAR, CITY branch on 01-02-2024. A/C Bal: Cr 120000.50",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("5000"), result?.amount)
        assertEquals(BigDecimal("120000.50"), result?.balance)
        assertEquals("SAMPLE BAZAR, CITY", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch credit with whole Cr balance is parsed with positive balance`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 7000 has been credited to your account *** 123 through SAMPLE BAZAR, CITY branch on 02-02-2024. A/C Bal: Cr 127000",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("7000"), result?.amount)
        assertEquals(BigDecimal("127000"), result?.balance)
        assertEquals("SAMPLE BAZAR, CITY", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch debit with double space before on is parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 3000 has been debited from your account *** 123 through SAMPLE ISLAMIC BANKING branch  on 03-02-2024. A/C Bal: Cr 124000.00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("3000"), result?.amount)
        assertEquals(BigDecimal("124000.00"), result?.balance)
        assertEquals("SAMPLE ISLAMIC BANKING", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `fund transfer credit with Cr balance and trailing period is parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 15000.00 has been credited to your account using FUND TRANSFER from SAMPLE ISLAMI BANK LIMITED on 04-JUL-24. A/C Bal:Cr 139000.00.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("15000.00"), result?.amount)
        assertEquals(BigDecimal("139000.00"), result?.balance)
        assertEquals("SAMPLE ISLAMI BANK LIMITED", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `eft credit with unmarked balance keeps its own sign`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 2500.00 has been credited to your a/c ***123 through EFT SAMPLE BANK LIMITE,REMITTANCE,NOTAPPLICABLE on 05-APR-24. A/C Bal:141500.00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("2500.00"), result?.amount)
        assertEquals(BigDecimal("141500.00"), result?.balance)
        assertEquals("EFT SAMPLE BANK LIMITE,REMITTANCE,NOTAPPLICABLE", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `pi banking credit is parsed with sender name and unmarked balance`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 2500.00 has been credited to your a/c ***123 from SAMPLE PERSON on 05-SEP-24 through PI Banking.A/C Bal:141500.00",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("2500.00"), result?.amount)
        assertEquals(BigDecimal("141500.00"), result?.balance)
        assertEquals("SAMPLE PERSON", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `Dr and Cr balances on the same account parse with opposite signs`() {
        val drResult = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 5000 has been debited from your account *** 123 through SAMPLE BAZAR, CITY branch on 01-02-2024. A/C Bal: Dr 120000.50",
            receivedAt = 0L
        )
        val crResult = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 5000 has been debited from your account *** 123 through SAMPLE BAZAR, CITY branch on 01-02-2024. A/C Bal: Cr 120000.50",
            receivedAt = 0L
        )

        assertEquals(BigDecimal("-120000.50"), drResult?.balance)
        assertEquals(BigDecimal("120000.50"), crResult?.balance)
    }

    @Test
    fun `bKash qr payment received is never parsed to avoid double counting`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "BDT 450.00 RECEIVED FROM 01700****00 ,bKash USING QR PAYMENT ON 05-SEP-24 .TrxID:AAAAAAAAAA.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `merchant qr settlement credit is never parsed to avoid double counting`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Dear Merchant! TK 450.00 has been credited for Merchant-QR transaction On Settlement Date 06-SEP-24. Stay with Pubali Bank.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `pi banking app promotion is never parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Dear Customer, you are requested to use Pubali PI Banking App. Enjoy easy Fund Transfer, Bill Payment, Mobile Recharge and more at your Fingertips",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `biometric registration notice is never parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Dear Customer, You are requested to visit your Branch for Bio-Metric Registration (Thumbs and Face) and enjoy Smart Banking.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `account number change notice is never parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Dear Sir, A/C 0000-000-00000 will change to 0000-000-00001 from 01/01/24, due to branch merging.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `thank you message is never parsed`() {
        val result = parser.parse(
            sender = "PUBALI BANK",
            body = "Dear Customer, thank you for banking with us",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns contain only the verified address`() {
        assertEquals(listOf("PUBALI BANK"), parser.senderPatterns)
    }
}
