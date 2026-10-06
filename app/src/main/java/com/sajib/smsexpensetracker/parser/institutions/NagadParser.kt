package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses Nagad wallet transaction SMS.
 *
 * Sender address is "NAGAD", verified in a third-party real SMS export. Not
 * yet verified on the project's own device.
 *
 * Anonymized sample bodies this was built against. Every number is invented,
 * only the wording and layout match the real messages. Fields are separated
 * by literal newlines:
 *
 * Cash In:
 *   "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\n
 *   TxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"
 *
 * Cash In with a promotional prefix and short labels:
 *   "WIN LAND IN DHAKA! CLICK NOW  nagad.io/xyz\nCash In Received.\n
 *   Amt:Tk 200.00\nUddokta:01700000002\nTxnID: 72BBBBBB\nBal:208.65\n
 *   27/03/2024 15:54"
 *
 * Money Received:
 *   "Money Received.\nAmount: Tk 20300.00\nSender: 01700000003\nRef: N/A\n
 *   TxnID: 73CCCCCC\nBalance: Tk 20300.32\n23/01/2025 20:58"
 *
 * Payment:
 *   "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\n
 *   TxnID: 72DDDDDD\nBalance: Tk 32.90\n03/11/2023 13:58"
 *
 * Promotional text can precede the transaction, so patterns are never
 * anchored to the start of the body. Following the bKash convention the
 * TxnID goes into reference and the agent (Uddokta), sender phone or
 * merchant goes into counterparty. The free-text Ref line has no field of
 * its own and is not captured.
 *
 * OTP and device registration messages and the Bangla fraud warning
 * intentionally match nothing and return null.
 */
class NagadParser : SmsParser {

    private companion object {
        const val SENDER = "NAGAD"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /** "Amount" or the short form "Amt", followed by an optional "Tk". */
        const val AMOUNT_LABEL = """(?:Amount|Amt):\s*(?:Tk\s*)?"""

        /** "Balance" or the short form "Bal", followed by an optional "Tk". */
        const val BALANCE_LABEL = """(?:Balance|Bal):\s*(?:Tk\s*)?"""

        const val TXN_ID = """TxnID:\s*(\w+)"""

        val cashInPattern = Regex(
            """Cash In Received\.\s*$AMOUNT_LABEL($AMOUNT)\s*Uddokta:\s*(\d+)\s*$TXN_ID\s*$BALANCE_LABEL($AMOUNT)"""
        )

        val moneyReceivedPattern = Regex(
            """Money Received\.\s*$AMOUNT_LABEL($AMOUNT)\s*Sender:\s*(\d+)\s*(?:Ref:[^\n]*\s*)?$TXN_ID\s*$BALANCE_LABEL($AMOUNT)"""
        )

        val paymentPattern = Regex(
            """Payment to '([^']+)' is Successful\.\s*$AMOUNT_LABEL($AMOUNT)\s*$TXN_ID\s*$BALANCE_LABEL($AMOUNT)"""
        )
    }

    override val institutionName = "Nagad"
    override val senderPatterns = listOf(SENDER)

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        cashInPattern.find(text)?.let { match ->
            val (amount, agent, txnId, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = agent,
                reference = txnId,
                rawSms = body
            )
        }

        moneyReceivedPattern.find(text)?.let { match ->
            val (amount, senderPhone, txnId, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = senderPhone,
                reference = txnId,
                rawSms = body
            )
        }

        paymentPattern.find(text)?.let { match ->
            val (merchant, amount, txnId, balance) = match.destructured
            return ParseResult(
                type = TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = merchant,
                reference = txnId,
                rawSms = body
            )
        }

        return null
    }

    /** Strips thousands-separator commas so BigDecimal parses cleanly. */
    private fun parseAmount(raw: String): BigDecimal =
        BigDecimal(raw.replace(",", ""))
}
