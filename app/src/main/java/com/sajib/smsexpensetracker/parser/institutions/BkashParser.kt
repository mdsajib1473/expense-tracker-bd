package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parses bKash transaction SMS.
 *
 * Real sample bodies this was built against (see project chat history for
 * the full set extracted from an SMS Backup & Restore XML export):
 *
 * Send Money success:
 *   "Send Money Tk 510.00 to 01813738081 successful. Ref Sajib. Fee Tk 5.00.
 *   Balance Tk 4,479.65. TrxID DCK1B9P81H at 20/03/2026 00:08"
 *
 * Receive (P2P), Ref and Fee both optional, sender can be a phone or a name:
 *   "You have received Tk 5,075.00 from 01735814791. Fee Tk 0.00. Balance
 *   Tk 6,075.50. TrxID BLU1H6X87P at 30/12/2024 13:22"
 *   "You have received Tk 63.00 from OnnorokomWeb. Balance Tk 1,082.65.
 *   TrxID DE971UR13F at 09/05/2026 21:32"
 *
 * Receive (bank deposit), treated as Add Money:
 *   "You have received a deposit of Tk 1,000.00 from Sonali Bank Account.
 *   Fee Tk 0.00. Balance Tk 1,140.34. TrxID CGJ2RQENX4 at 19/07/2025 15:43"
 *
 * Cash In (agent), also Add Money:
 *   "Cash In Tk 1,000.00 from 01835908735 successful. Fee Tk 0.00. Balance
 *   Tk 1,607.90. TrxID CK710WRNDB at 07/11/2025 22:21. Download App: ..."
 *
 * Payment (merchant), two real phrasings, never has a Fee:
 *   "Payment Tk 100.00 to KAHF BANGLADESH LTD-RM67912 is successful.
 *   Balance Tk 601.81. TrxID DG705TRZHM at 07/07/2026 20:36"
 *   "Payment of Tk 1,015.50 to AS-SUNNAH FOUNDATION-RM56187 is successful.
 *   Balance Tk 1,115.15. TrxID DCD92UO50N at 13/03/2026 15:30"
 *
 * A failed Send Money ("Sorry, your Send Money request ... is unsuccessful!
 * Tk X has been returned...") intentionally matches nothing here and falls
 * through to null: the money was returned, so the wallet's net balance is
 * unchanged and there is no real transaction to record. Recording it would
 * risk a false debit entry.
 *
 * The bKash OTP message ("Your bKash verification code is ...") also
 * intentionally matches nothing and returns null, since it is not a
 * transaction and must never be stored.
 */
class BkashParser : SmsParser {

    override val institutionName = "bKash"
    override val senderPatterns = listOf("bKash", "01678600000")

    override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val text = body.trim()

        sendMoneyPattern.find(text)?.let { match ->
            val (amount, _, _, balance, trxId) = match.destructured
            return ParseResult(
                type = TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = match.groupValues[2],
                reference = trxId,
                rawSms = body
            )
        }

        receiveBankDepositPattern.find(text)?.let { match ->
            val (amount, _, _, balance, trxId) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = "Sonali Bank Account",
                reference = trxId,
                rawSms = body
            )
        }

        receiveP2pPattern.find(text)?.let { match ->
            val amount = match.groupValues[1]
            val counterparty = match.groupValues[2]
            val balance = match.groupValues[3]
            val trxId = match.groupValues[4]
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = counterparty,
                reference = trxId,
                rawSms = body
            )
        }

        cashInPattern.find(text)?.let { match ->
            val (amount, phone, _, balance, trxId) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = phone,
                reference = trxId,
                rawSms = body
            )
        }

        paymentPattern.find(text)?.let { match ->
            val (amount, merchant, balance, trxId) = match.destructured
            return ParseResult(
                type = TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = merchant,
                reference = trxId,
                rawSms = body
            )
        }

        return null
    }

    /** Strips thousands-separator commas so BigDecimal parses cleanly. */
    private fun parseAmount(raw: String): BigDecimal =
        BigDecimal(raw.replace(",", ""))

    private companion object {
        val sendMoneyPattern = Regex(
            """Send Money Tk ([\d,]+\.?\d*) to (\d+) successful\.(?: Ref [^.]+\.)? Fee Tk ([\d,]+\.?\d*)\. Balance Tk ([\d,]+\.?\d*)\. TrxID (\S+) at"""
        )

        val receiveBankDepositPattern = Regex(
            """You have received a deposit of Tk ([\d,]+\.?\d*) from (Sonali Bank Account)\. Fee Tk ([\d,]+\.?\d*)\. Balance Tk ([\d,]+\.?\d*)\. TrxID (\S+) at"""
        )

        val receiveP2pPattern = Regex(
            """You have received Tk ([\d,]+\.?\d*) from ([^.]+?)\.\s*(?:Ref [^.]+\.\s*)?(?:Fee Tk [\d,]+\.?\d*\.\s*)?Balance Tk ([\d,]+\.?\d*)\. TrxID (\S+) at"""
        )

        val cashInPattern = Regex(
            """Cash In Tk ([\d,]+\.?\d*) from (\d+) successful\. Fee Tk ([\d,]+\.?\d*)\. Balance Tk ([\d,]+\.?\d*)\. TrxID (\S+) at"""
        )

        val paymentPattern = Regex(
            """Payment (?:of )?Tk ([\d,]+\.?\d*) to (.+?) is successful\. Balance Tk ([\d,]+\.?\d*)\. TrxID (\S+) at"""
        )
    }
}