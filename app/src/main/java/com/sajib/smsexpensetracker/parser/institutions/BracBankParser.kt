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
 * Withdrawal, deposit, credit and debit:
 *   "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22.
 *   Your A/C balance is TK 29,630.27. For Enquiry call: 16221"
 *   "TK 38,750 has been credited to your A/C# 130320**2001 on 25-08-22.
 *   Your A/C balance is TK 56,728.77. For Enquiry call: 16221"
 *
 * Branch counter deposit and withdrawal:
 *   "TK 28,750.00 has been deposited to A/C#13032**2001 on 22-09-22, 11:21
 *   AM at SAMPLE SME BRANCH. Balance TK 46,743.77. Query: 16221."
 *   "TK 9,000.00 has been withdrawn from BRAC Bank A/C#13032**2001 on
 *   04-05-23, 02:10 PM at SAMPLE SME/KRISHI BRANCH. Balance TK 37,743.77.
 *   Query: 16221"
 *
 * Incoming transfer from another bank:
 *   "TK 5,000.00 credited to A/C#13032**2001 on 06-05-23 @10:15 AM from
 *   OTHER BANK. Balance TK 42,743.77. BRAC Bank."
 *
 * Loan-linked account deposit and RTGS debit:
 *   "TK 30,000.00 deposited to your loan-linked A/C 1303***2001 on 09/05/23
 *   4:05 pm at AB Outlet. Your balance is TK 50,000.00. Helpline 16221"
 *   "TK 100,500.00 (incl. charges) has been debited from your A/C 130**2001
 *   through RTGS on 08-05-23. Available balance: TK 20,000.00. Helpline:
 *   16221"
 *
 * Cash deposit:
 *   "CASH DEPOSIT of TK 4,000.00 to your account 1303***2001 was successful
 *   on 07/05/23 3:30 pm. Your new balance is TK 46,743.77 . Helpline 16221"
 *
 * Cheque clearing credit:
 *   "Dear Customer, clearing cheque No. 6525300 of BDT 26,600.00 has been
 *   deposited to A/C#130320**2001.Your A/C balance is TK 70,130.27.Enquiry:
 *   16221."
 *
 * The letter case of "Tk" and "TK" varies, so matching ignores case. The
 * account mask differs between messages and is matched loosely, never
 * stored. Loan instalment reminders, confirmations and disbursements,
 * overdue and dormancy notices, cheque book notices and OTP messages
 * intentionally match nothing and return null.
 */
class BracBankParser : SmsParser {

    private companion object {
        const val SENDER = "BRAC BANK"

        /** Amount with optional thousands commas and an optional decimal part. */
        const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

        /**
         * Account reference in any of the real forms: "A/C#130320**2001",
         * "A/C# 130320**2001", "BRAC Bank A/C#13032**2001",
         * "loan-linked A/C 1303***2001" or "A/C 130**2001".
         */
        const val ACCOUNT = """(?:your\s+)?(?:loan-linked\s+)?(?:BRAC\s+Bank\s+)?A/C\s*#?\s*[\d*]+"""

        /**
         * Date and optional time as printed by BRAC Bank, for example
         * "22-03-22", "09/05/23", "22-09-22, 11:21 AM" or "06-05-23 @10:15 AM".
         * Kept only as structural anchors; the transaction time comes from
         * the SMS timestamp supplied by the caller.
         */
        const val DATE = """\d{1,2}[-/]\d{1,2}[-/]\d{2,4}"""
        const val TIME = """,?\s*@?\s*\d{1,2}:\d{2}\s*[AP]M"""

        const val BALANCE_LABEL = """(?:Your\s+A/C\s+balance\s+is|Your\s+balance\s+is|Available\s+balance\s*:|Balance)"""

        /**
         * Direction phrase in group 2, optional "through" channel in group 3,
         * optional "at"/"from" branch or source in group 4, closing balance
         * in group 5.
         */
        val accountAlertPattern = Regex(
            """Tk\s*($AMOUNT)\s*(?:\(incl\.\s*charges\)\s*)?(?:has\s+been\s+)?(withdrawn\s+from|deposited\s+to|credited\s+to|debited\s+from)\s+$ACCOUNT\s+(?:through\s+(.+?)\s+)?on\s+$DATE(?:$TIME)?(?:\s+(?:at|from)\s+(.+?))?\s*\.\s*$BALANCE_LABEL\s*Tk\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        val cashDepositPattern = Regex(
            """CASH\s+DEPOSIT\s+of\s+Tk\s*($AMOUNT)\s+to\s+your\s+account\s*[\d*]+\s+was\s+successful\s+on\s+$DATE(?:$TIME)?\s*\.\s*Your\s+new\s+balance\s+is\s+Tk\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        val chequeClearingPattern = Regex(
            """clearing\s+cheque\s+No\.\s*(\d+)\s+of\s+BDT\s*($AMOUNT)\s+has\s+been\s+deposited\s+to\s+$ACCOUNT\s*\.\s*Your\s+A/C\s+balance\s+is\s+Tk\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        )

        val creditDirectionPrefixes = listOf("deposited", "credited")
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

        cashDepositPattern.find(text)?.let { match ->
            val (amount, balance) = match.destructured
            return ParseResult(
                type = TransactionType.CREDIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = null,
                reference = null,
                rawSms = body
            )
        }

        accountAlertPattern.find(text)?.let { match ->
            val (amount, direction, channel, source, balance) = match.destructured
            val isCredit = creditDirectionPrefixes.any { direction.startsWith(it, ignoreCase = true) }
            return ParseResult(
                type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT,
                amount = parseAmount(amount),
                balance = parseAmount(balance),
                counterparty = channel.ifEmpty { source }.ifEmpty { null },
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
