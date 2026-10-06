package com.sajib.smsexpensetracker.parser.core

import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import com.sajib.smsexpensetracker.parser.institutions.BracBankParser
import com.sajib.smsexpensetracker.parser.institutions.DutchBanglaParser
import com.sajib.smsexpensetracker.parser.institutions.NagadParser
import com.sajib.smsexpensetracker.parser.institutions.PubaliBankParser
import com.sajib.smsexpensetracker.parser.institutions.UttaraBankParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers routing only: an SMS reaches the parser whose sender pattern
 * matches, never another institution's parser. Bodies are anonymized,
 * every number in them is invented.
 */
class ParserEngineTest {

    private val allParsers = ParserEngine(
        parsers = listOf(
            BkashParser(),
            DutchBanglaParser(),
            UttaraBankParser(),
            PubaliBankParser(),
            NagadParser(),
            BracBankParser()
        )
    )

    private val uttaraBody =
        "Dear Customer, BDT 47800 has been credited to your A/C No 032300000000001 from Sample Road Branch and Current Balance is: BDT -1452200"
    private val pubaliBody =
        "BDT 50000 has been debited from your account ***865 through SAMPLE BAZAR branch on 24-08-2023. A/C Bal: Dr 434061.55"
    private val nagadBody =
        "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"
    private val bracBody =
        "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22. Your A/C balance is TK 29,630.27. For Enquiry call: 16221"

    private val bodiesBySender = mapOf(
        "UTTARA BANK" to uttaraBody,
        "PUBALI BANK" to pubaliBody,
        "NAGAD" to nagadBody,
        "BRAC BANK" to bracBody
    )

    @Test
    fun `parse returns null when no parsers are registered`() {
        val engine = ParserEngine(parsers = emptyList())

        val result = engine.parse(
            sender = "bKash",
            body = "You have received Tk 500.00 from 017XXXXXXXX",
            receivedAt = System.currentTimeMillis()
        )

        assertNull(result)
    }

    @Test
    fun `each sender routes to its own parser`() {
        val expectedInstitution = mapOf(
            "UTTARA BANK" to "Uttara Bank",
            "PUBALI BANK" to "Pubali Bank",
            "NAGAD" to "Nagad",
            "BRAC BANK" to "BRAC Bank"
        )

        bodiesBySender.forEach { (sender, body) ->
            val result = allParsers.parse(sender, body, receivedAt = 0L)
            assertEquals(sender, expectedInstitution[sender], result?.institutionName)
        }
    }

    @Test
    fun `a sender never routes another institution body to a parser`() {
        bodiesBySender.forEach { (sender, _) ->
            bodiesBySender
                .filterKeys { it != sender }
                .forEach { (otherSender, otherBody) ->
                    assertNull(
                        "$otherSender body under sender $sender",
                        allParsers.parse(sender, otherBody, receivedAt = 0L)
                    )
                }
        }
    }

    @Test
    fun `notification only sender BRACBANK with a BRAC transaction body returns null`() {
        val result = allParsers.parse(
            sender = "BRACBANK",
            body = bracBody,
            receivedAt = 0L
        )

        assertNull(result)
    }
}
