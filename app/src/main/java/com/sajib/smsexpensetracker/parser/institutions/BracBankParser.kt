package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses BRAC Bank account alert SMS.
 *
 * Sender address is "BRAC BANK" (with the space), verified in a real SMS
 * export. Not yet verified on the project's own device. The sender
 * "BRACBANK" (no space) only sends notifications and must never reach this
 * parser, so "BRAC" or "BRACB" must never be used as a sender pattern: the
 * engine matches senders by substring.
 *
 * Anonymized sample bodies this was built against. Every number and name is
 * invented, only the wording and layout match the real messages:
 *
 * Withdrawal and deposit:
 *   "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22.
 *   Your A/C balance is TK 29,630.27. For Enquiry call: 16221"
 *   "Tk 38,740 has been deposited to your A/C#130320**2001 on 23-03-22.
 *   Your A/C balance is TK 68,370.27. For Enquiry call: 16221"
 *
 * Credit and debit:
 *   "Tk 38,750 has been credited to your A/C#130320**2001 on 25-08-22.
 *   Your A/C balance is TK 56,728.77. For Enquiry call: 16221"
 *   "Tk 38,735 has been debited from your A/C#130320**2001 on 25-09-22.
 *   Your A/C balance is TK 8,008.77. For enquiry call: 16221"
 *
 * Branch deposit and withdrawal:
 *   "TK 28,750.00 has been deposited to A/C#13032**2001 on 22-09-22, 11:21
 *   AM at SAMPLE SME BRANCH. Balance TK 46,743.77. Query: 16221."
 *   "TK 50,000.00 has been withdrawn from A/C#13032**2001 on 19-12-22, 12:17
 *   PM at SAMPLE SME BRANCH. Balance TK 7,793.77. Query: 16221."
 *
 * Cheque clearing credit:
 *   "Dear Customer, clearing cheque No. 6525300 of BDT 26,600.00 has been
 *   deposited to A/C#130320**2001.Your A/C balance is TK 70,130.27.Enquiry:
 *   16221."
 *
 * The letter case of "Tk" and "TK" varies, so matching ignores case. The
 * account mask differs between messages and is matched loosely, never
 * stored. Loan installment reminders and confirmations, overdue notices and
 * OTP messages intentionally match nothing and return null.
 */
class BracBankParser : SmsParser {

    private companion object {
        const val SENDER = "BRAC BANK"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /** Masked account, for example "A/C#130320**2001" or "A/C#13032**2001". */
        const val MASKED_ACCOUNT = """A/C#\s*[\d*]+"""

        /**
         * Date and optional time as printed by BRAC Bank, for example
         * "22-03-22" or "22-09-22, 11:21 AM". Kept only as a structural
         * anchor; the transaction time comes from the SMS timestamp
         * supplied by the caller.
         */
        const val DATE = """\d{2}-\d{2}-\d{2,4}"""
        const val TIME = """\d{1,2}:\d{2}\s*[AP]M"""

        /**
         * Direction phrase in group 2, optional branch in group 3, closing
         * balance in group 4. Both the "Your A/C balance is" and the shorter
         * branch-counter "Balance" wording are accepted.
         */
        val accountAlertPattern = Regex(
            """Tk\s*($AMOUNT) has been (withdrawn from|deposited to|credited to|debited from) (?:your )?$MASKED_ACCOUNT on $DATE(?:,\s*$TIME)?(?: at (.+?))?\.\s*(?:Your A/C balance is|Balance) Tk\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        val chequeClearingPattern = Regex(
            """clearing cheque No\.\s*(\d+) of BDT\s*($AMOUNT) has been deposited to $MASKED_ACCOUNT\.\s*Your A/C balance is Tk\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        val creditDirections = setOf("deposited to", "credited to")
    }

    override val institutionName = "BRAC Bank"
    override val senderPatterns = listOf(SENDER)

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        chequeClearingPattern.find(text)?.let { match ->
            val (chequeNumber, amount, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = null,
                reference = chequeNumber,
                rawSms = body
            )
        }

        accountAlertPattern.find(text)?.let { match ->
            val (amount, direction, branch, balance) = match.destructured
            return ParseResult(
                type = if (direction.lowercase() in creditDirections) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                },
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = branch.ifEmpty { null },
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
