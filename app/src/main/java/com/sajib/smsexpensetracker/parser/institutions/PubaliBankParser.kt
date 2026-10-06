package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses Pubali Bank account alert SMS.
 *
 * Sender address is "PUBALI BANK", verified in a real SMS export. Not yet
 * verified on the project's own device.
 *
 * Anonymized sample bodies this was built against. Every number and name is
 * invented, only the wording and layout match the real messages:
 *
 * Branch credit:
 *   "BDT 140000 has been credited to your account *** 865 through SAMPLE
 *   BAZAR, CITY branch on 20-08-2023. A/C Bal: Dr 334050.05"
 *
 * Branch debit:
 *   "BDT 50000 has been debited from your account ***865 through SAMPLE BAZAR
 *   branch on 24-08-2023. A/C Bal: Dr 434061.55"
 *
 * EFT credit:
 *   "BDT 4374.78 has been credited to your a/c ***2865 through EFT SAMPLE BANK
 *   LIMITE,REMITTANCE,SAMPLEBANKLTD on 14-NOV-23. A/C Bal:-283050.77"
 *
 * Fund transfer credit:
 *   "BDT 70000.00 has been credited to your account using FUND TRANSFER from
 *   SAMPLE COMMERCIAL BANK LIMITED on 24-DEC-23. A/C Bal:Dr 330365.72."
 *
 * The account mask varies ("*** 865", "***865", "***2865") and is matched
 * loosely, never stored. A "Dr" marker before the balance means a debit
 * (overdrawn) balance and is stored as a negative figure. A balance that
 * already carries a minus sign stays negative.
 *
 * The cheque book collection notice intentionally matches nothing and
 * returns null.
 */
class PubaliBankParser : SmsParser {

    private companion object {
        const val SENDER = "PUBALI BANK"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /** Masked account, for example "*** 865", "***865" or "***2865". */
        const val MASKED_ACCOUNT = """\*+\s*\d+"""

        /**
         * Date as printed by Pubali, for example "20-08-2023" or "14-NOV-23".
         * Kept only as a structural anchor; the transaction time comes from
         * the SMS timestamp supplied by the caller.
         */
        const val DATE = """\d{2}-(?:\d{2}|[A-Za-z]{3})-\d{2,4}"""

        const val DEBIT_BALANCE_MARKER = "Dr"

        /**
         * "credited to" or "debited from" in group 1, the source in group 2
         * (branch, EFT originator or fund transfer bank), the optional "Dr"
         * marker in group 3 and the possibly signed balance in group 4.
         */
        val transactionPattern = Regex(
            """BDT\s*($AMOUNT) has been (credited to|debited from) your (?:account|a/c)\s*(?:$MASKED_ACCOUNT\s*)?(?:through (.+?)(?: branch)?|using FUND TRANSFER from (.+?)) on $DATE\.\s*A/C Bal:\s*($DEBIT_BALANCE_MARKER)?\s*(-?$AMOUNT)""",
            RegexOption.IGNORE_CASE
        )
    }

    override val institutionName = "Pubali Bank"
    override val senderPatterns = listOf(SENDER)

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        transactionPattern.find(text)?.let { match ->
            val amount = match.groupValues[1]
            val direction = match.groupValues[2]
            val source = match.groupValues[3].ifEmpty { match.groupValues[4] }
            val hasDebitMarker = match.groupValues[5].isNotEmpty()
            val balance = parseAmount(match.groupValues[6])
            return ParseResult(
                type = if (direction.startsWith("credited", ignoreCase = true)) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                },
                amount = parseAmount(amount),
                balance = if (hasDebitMarker) balance.abs().negate() else balance,
                counterparty = source,
                reference = null,
                rawSms = body
            )
        }

        return null
    }

    /** Strips thousands-separator commas so BigDecimal parses cleanly. */
    private fun parseAmount(raw: String): BigDecimal =
        BigDecimal(raw.replace(",", ""))
}
