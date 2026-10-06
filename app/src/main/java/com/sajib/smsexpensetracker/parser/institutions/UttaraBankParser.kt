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
 * only the wording and layout match the real messages:
 *
 * Credit:
 *   "Dear Customer, BDT 47800 has been credited to your A/C No
 *   032300000000001 from Sample Road Branch and Current Balance is:
 *   BDT -1452200"
 *
 * Debit:
 *   "Dear Customer, BDT 200000 has been debited from your A/C No
 *   032300000000002 from Sample Branch and Current Balance is:
 *   BDT -200258.75"
 *
 * The balance can be negative (overdraft or loan account), so a leading
 * minus sign is kept.
 *
 * Non-transaction messages from the same sender intentionally match nothing
 * and return null: the SMS banking account-created message (it carries a PIN
 * and must never be stored or logged), the loan instalment due reminder and
 * the cheque book ready notice.
 *
 * The full account number is never copied into the result. The branch named
 * after "from" is used as the counterparty.
 */
class UttaraBankParser : SmsParser {

    private companion object {
        const val SENDER = "UTTARA BANK"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /** Balance as printed by Uttara Bank, which may carry a leading minus. */
        const val SIGNED_AMOUNT = """-?$AMOUNT"""

        const val ACCOUNT_NUMBER = """[\d-]+"""

        val creditPattern = Regex(
            """BDT\s*($AMOUNT) has been credited to your A/C No\s*$ACCOUNT_NUMBER from (.+?) and Current Balance is:\s*BDT\s*($SIGNED_AMOUNT)"""
        )

        val debitPattern = Regex(
            """BDT\s*($AMOUNT) has been debited from your A/C No\s*$ACCOUNT_NUMBER from (.+?) and Current Balance is:\s*BDT\s*($SIGNED_AMOUNT)"""
        )
    }

    override val institutionName = "Uttara Bank"
    override val senderPatterns = listOf(SENDER)

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        creditPattern.find(text)?.let { match ->
            val (amount, branch, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = branch,
                reference = null,
                rawSms = body
            )
        }

        debitPattern.find(text)?.let { match ->
            val (amount, branch, balance) = match.destructured
            return ParseResult(
                type = TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = branch,
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
