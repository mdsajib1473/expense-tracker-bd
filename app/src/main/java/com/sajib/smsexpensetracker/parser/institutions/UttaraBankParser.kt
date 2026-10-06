package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses Uttara Bank account alert SMS.
 *
 * Sender address is "UTTARA BANK", verified in a real SMS export. Not yet
 * verified on the project's own device.
 *
 * Anonymized sample bodies this was built against. Every number is invented,
 * only the wording and layout match the real messages. Spacing in the real
 * messages is irregular (double spaces, "fromX", "is:BDT"), so every gap is
 * matched as flexible whitespace:
 *
 * Credit:
 *   "Dear Customer, BDT 47800 has been credited to your A/C No
 *   032300000000001 from Sample Road Branch and Current Balance is:
 *   BDT -1452200"
 *   "Dear Customer, BDT 14000  has been credited to your A/C No
 *   032300000000003 from Head Office and Current Balance is: BDT 52000.50"
 *
 * Debit:
 *   "Dear Customer, BDT 200000 has been debited from your A/C No
 *   032300000000002 from Sample Branch and Current Balance is:
 *   BDT -200258.75"
 *
 * Service charge debit (the model has no fee type, so this is a plain debit):
 *   "Dear Customer, BDT 3000.50 has been debited from your A/C No 0323****0003
 *   for SMS Service Charge  fromSample Road Branch and Current Balance
 *   is:BDT 48999.50"
 *
 * Credit with no balance (balance is null):
 *   "Dear Customer, BDT 5000 has been credited to your A/C No 032300000000003
 *   from Sample Road Branch"
 *
 * Balance statement, mapped to BALANCE_INQUIRY like DBBL, so amount and
 * balance hold the same figure:
 *   "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk.
 *   12345.67 . For details, please contact your Branch."
 *
 * The balance can be negative (overdraft or loan account), so a leading
 * minus sign is kept.
 *
 * Non-transaction messages from the same sender intentionally match nothing
 * and return null: the SMS banking account-created message (it carries a PIN
 * and must never be stored or logged), loan instalment due reminders, the
 * cheque book ready notice, the contact center notice, Bangla security
 * warnings and promotions, and the RTGS "has been sent" notice. The RTGS
 * notice is skipped on purpose: the bank sends a separate debit alert with
 * the balance for the same transfer, so parsing both would double count.
 *
 * The account number is never copied into the result. The text after "from"
 * is used as the counterparty and need not contain the word "Branch".
 */
class UttaraBankParser : SmsParser {

    private companion object {
        const val SENDER = "UTTARA BANK"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /** Balance as printed by Uttara Bank, which may carry a leading minus. */
        const val SIGNED_AMOUNT = """-?$AMOUNT"""

        /** Full or masked account number, for example "0323****0003". */
        const val ACCOUNT_NUMBER = """[\d*-]+"""

        /**
         * Statement date, for example "1/2/2024". Kept only as a structural
         * anchor; the time comes from the SMS timestamp supplied by the caller.
         */
        const val DATE = """\d{1,2}/\d{1,2}/\d{2,4}"""

        /**
         * Amount in group 1, direction in group 2, the source after "from" in
         * group 3 and the optional balance in group 4. An optional
         * "for <purpose>" clause before "from" is matched and dropped.
         */
        val transactionPattern = Regex(
            """BDT\s*($AMOUNT)\s*has\s+been\s+(credited\s+to|debited\s+from)\s+your\s+A/C\s+No\.?\s*$ACCOUNT_NUMBER\s*(?:for\s+.+?\s*)?from\s*(.+?)\s*(?:and\s+Current\s+Balance\s+is\s*:\s*BDT\s*($SIGNED_AMOUNT)|$)""",
            RegexOption.IGNORE_CASE
        )

        val balanceStatementPattern = Regex(
            """balance\s+of\s+your\s+A/C\s+No\.?\s*$ACCOUNT_NUMBER\s+as\s+on\s+$DATE\s+is\s+Tk\.?\s*($SIGNED_AMOUNT)""",
            RegexOption.IGNORE_CASE
        )
    }

    override val institutionName = "Uttara Bank"
    override val senderPatterns = listOf(SENDER)

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        balanceStatementPattern.find(text)?.let { match ->
            val reported = parseAmount(match.groupValues[1])
            return ParseResult(
                type = TransactionType.BALANCE_INQUIRY,
                amount = reported,
                balance = reported,
                counterparty = null,
                reference = null,
                rawSms = body
            )
        }

        transactionPattern.find(text)?.let { match ->
            val (amount, direction, source, balance) = match.destructured
            return ParseResult(
                type = if (direction.startsWith("credited", ignoreCase = true)) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                },
                amount = parseAmount(amount),
                balance = balance.ifEmpty { null }?.let(::parseAmount),
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
