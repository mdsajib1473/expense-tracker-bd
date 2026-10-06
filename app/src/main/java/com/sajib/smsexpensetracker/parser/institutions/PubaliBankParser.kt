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
 * Branch credit and debit:
 *   "BDT 140000 has been credited to your account *** 865 through SAMPLE
 *   BAZAR, CITY branch on 20-08-2023. A/C Bal: Dr 334050.05"
 *   "BDT 5000 has been debited from your account *** 123 through SAMPLE
 *   BAZAR, CITY branch on 01-02-2024. A/C Bal: Cr 120000.50"
 *   "BDT 3000 has been debited from your account *** 123 through SAMPLE
 *   ISLAMIC BANKING branch  on 03-02-2024. A/C Bal: Cr 124000.00"
 *
 * EFT credit:
 *   "BDT 4374.78 has been credited to your a/c ***2865 through EFT SAMPLE BANK
 *   LIMITE,REMITTANCE,SAMPLEBANKLTD on 14-NOV-23. A/C Bal:-283050.77"
 *
 * Fund transfer credit:
 *   "BDT 70000.00 has been credited to your account using FUND TRANSFER from
 *   SAMPLE COMMERCIAL BANK LIMITED on 24-DEC-23. A/C Bal:Dr 330365.72."
 *
 * PI Banking credit (no space before "A/C Bal" in the real format):
 *   "BDT 2500.00 has been credited to your a/c ***123 from SAMPLE PERSON on
 *   05-SEP-24 through PI Banking.A/C Bal:141500.00"
 *
 * The account mask varies ("*** 865", "***865", "***2865") and is matched
 * loosely, never stored. Balance sign rules: "Dr" before the balance means
 * a debit (overdrawn) balance and is stored negative, "Cr" means a credit
 * balance and is stored positive, and a balance with no marker keeps its
 * own sign. The balance may or may not end with a period.
 *
 * Messages that intentionally return null:
 * - bKash QR payment received ("BDT 450.00 RECEIVED FROM ... USING QR
 *   PAYMENT") and the Merchant-QR settlement credit ("Dear Merchant! TK
 *   450.00 has been credited for Merchant-QR transaction"): neither carries
 *   a balance, and both describe the same money at payment and at
 *   settlement, so parsing both would double count.
 * - Cheque book, PI Banking app, biometric registration, account number
 *   change, thank-you and maintenance notices: not transactions.
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
        const val DATE = """\d{1,2}-(?:\d{1,2}|[A-Za-z]{3})-\d{2,4}"""

        const val DEBIT_BALANCE_MARKER = "Dr"
        const val CREDIT_BALANCE_MARKER = "Cr"

        /**
         * "credited to" or "debited from" in group 2, the source in group 3
         * or 4 (branch or EFT originator, or fund transfer bank), the
         * optional "Dr"/"Cr" marker in group 5 and the possibly signed
         * balance in group 6.
         */
        val transactionPattern = Regex(
            """BDT\s*($AMOUNT)\s*has\s+been\s+(credited\s+to|debited\s+from)\s+your\s+(?:account|a/c)\s*(?:$MASKED_ACCOUNT\s*)?(?:through\s+(.+?)(?:\s+branch)?|using\s+FUND\s+TRANSFER\s+from\s+(.+?))\s+on\s+$DATE\s*\.\s*A/C\s*Bal\s*:\s*($DEBIT_BALANCE_MARKER|$CREDIT_BALANCE_MARKER)?\s*(-?$AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        /**
         * PI Banking credit: sender name in group 2, balance in group 3. This
         * format carries no Dr or Cr marker, so the balance keeps its printed
         * sign. The real message has no space between "PI Banking." and
         * "A/C Bal", so that gap is optional.
         */
        val piBankingCreditPattern = Regex(
            """BDT\s*($AMOUNT)\s*has\s+been\s+credited\s+to\s+your\s+(?:account|a/c)\s*(?:$MASKED_ACCOUNT\s*)?from\s+(.+?)\s+on\s+$DATE\s+through\s+PI\s+Banking\s*\.\s*A/C\s*Bal\s*:\s*(-?$AMOUNT)""",
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
            val marker = match.groupValues[5]
            val balance = parseAmount(match.groupValues[6])
            return ParseResult(
                type = if (direction.startsWith("credited", ignoreCase = true)) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                },
                amount = parseAmount(amount),
                balance = when {
                    marker.equals(DEBIT_BALANCE_MARKER, ignoreCase = true) -> balance.abs().negate()
                    marker.equals(CREDIT_BALANCE_MARKER, ignoreCase = true) -> balance.abs()
                    else -> balance
                },
                counterparty = source,
                reference = null,
                rawSms = body
            )
        }

        piBankingCreditPattern.find(text)?.let { match ->
            val (amount, senderName, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = senderName,
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
