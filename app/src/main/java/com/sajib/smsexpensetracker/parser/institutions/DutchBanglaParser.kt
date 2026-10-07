package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses Dutch-Bangla Bank (DBBL) account alert SMS.
 *
 * Sender is "16216" on the real device. Note that "16216" was once assumed
 * to be Rocket's sender code; every real message seen from it so far is a
 * DBBL bank alert, not a Rocket wallet transaction (see Roadmap.md, M4).
 * This parser therefore matches on message wording, never on sender alone.
 *
 * Anonymized sample bodies this was built against. Wording, layout and
 * spacing match the real messages from the device inbox; every account
 * mask, amount, balance, TxnId and OTP is invented:
 *
 * Balance inquiry (several real samples, only date and amount differ):
 *   "Balance of your A/C:***123 is BDT 45,000.00 as on 30/06/25. To download
 *   statement click https://app.dutchbanglabank.com/cbsstatement . For query
 *   call 16216"
 *   "Balance of your A/C:***123 is BDT 450.00 as on 31/12/24. To download
 *   statement click https://app.dutchbanglabank.com/cbsstatement . For query
 *   call 16216"
 *
 * ATM-to-A/C transfer credit:
 *   "Dear Sir, your A/C ***123 credited (ATM A/C to A/C Transfer Credit) by
 *   Tk50,000.00 on 01-02-2025 02:55:44 PM C/B Tk50,450.00. NexusPay
 *   https://bit.ly/nexuspay"
 *
 * NexusPay cash-out debit. The missing spaces between fields are the real
 * format as sent by the bank, not a transcription error:
 *   "Cash-Out to A/C:***456Tk900.00Fee:Tk15.00 Your A/C Balance:
 *   Tk300.00.TxnId:1111111111Date:27-SEP-25 05:59:38 pm. Please download
 *   https://bit.ly/nexuspay"
 *
 * The debit card OTP message ("Dear Customer, 000000 is your OTP for e-com
 * transaction of Debit Card no# ...") intentionally matches nothing and
 * returns null, since it is not a transaction and must never be stored.
 *
 * For a balance inquiry there is no separate transaction amount, so amount
 * and balance deliberately hold the same figure for that type only.
 */
class DutchBanglaParser : SmsParser {

    override val institutionName = "Dutch-Bangla Bank"
    override val senderPatterns = listOf("16216", "DUTCHB")

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        balanceInquiryPattern.find(text)?.let { match ->
            val reported = parseAmount(match.groupValues[2])
            return ParseResult(
                type = TransactionType.BALANCE_INQUIRY,
                amount = reported,
                balance = reported,
                counterparty = null,
                reference = null,
                rawSms = body
            )
        }

        atmTransferCreditPattern.find(text)?.let { match ->
            val (_, description, amount, _, closingBalance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(closingBalance),
                counterparty = description,
                reference = null,
                rawSms = body
            )
        }

        cashOutPattern.find(text)?.let { match ->
            val (destinationAccount, amount, _, balance, txnId) = match.destructured
            return ParseResult(
                type = TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = destinationAccount,
                reference = txnId,
                rawSms = body
            )
        }

        return null
    }

    /** Strips thousands-separator commas so BigDecimal parses cleanly. */
    private fun parseAmount(raw: String): BigDecimal =
        BigDecimal(raw.replace(",", ""))

    private companion object {
        /**
         * Amount with optional thousands commas and an optional two-digit
         * decimal part. The decimal part is fixed at two digits on purpose:
         * in the cash-out format the balance is followed directly by a
         * sentence-ending period ("Tk300.00.TxnId"), so an open-ended
         * "\.?\d*" would be ambiguous about where the number stops.
         */
        const val AMOUNT = """[\d,]+(?:\.\d{2})?"""

        /** Masked account as printed by DBBL, for example "***123" or "***456". */
        const val MASKED_ACCOUNT = """\*+\d+"""

        val balanceInquiryPattern = Regex(
            """Balance of your A/C:\s*($MASKED_ACCOUNT) is BDT\s*($AMOUNT) as on (\d{2}/\d{2}/\d{2,4})"""
        )

        val atmTransferCreditPattern = Regex(
            """your A/C ($MASKED_ACCOUNT) credited \(([^)]+)\) by Tk\s*($AMOUNT) on (\d{2}-\d{2}-\d{4} \d{2}:\d{2}:\d{2} [AP]M) C/B Tk\s*($AMOUNT)"""
        )

        /**
         * Fields in the real message run together with no separators
         * ("***456Tk900.00Fee:Tk15.00"). Each boundary is still unambiguous
         * because the masked account ends in digits and every amount is
         * introduced by the literal "Tk", so the regex anchors on those
         * literals rather than on whitespace. "\s*" between fields tolerates
         * either the real no-space form or a spaced variant.
         */
        val cashOutPattern = Regex(
            """Cash-Out to A/C:\s*($MASKED_ACCOUNT)\s*Tk\s*($AMOUNT)\s*Fee:\s*Tk\s*($AMOUNT)\s*Your A/C Balance:\s*Tk\s*($AMOUNT)\.?\s*TxnId:\s*(\d+)"""
        )
    }
}
