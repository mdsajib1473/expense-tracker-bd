package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.ParserTestMessages
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Crafted bodies whose wording matches a real template but whose amount or
 * balance text is not a number: a lone comma, no digits after the currency
 * marker, several dots, digits replaced by separators, or a message cut
 * short. A parser must answer "not recognized" (null) for each and must
 * never throw, because SMS text is untrusted input. Every account, phone
 * number, TrxID and amount is invented.
 */
@RunWith(Parameterized::class)
class CraftedAmountParserTest(
    private val institutionName: String,
    private val case: String,
    private val body: String
) {

    @Test
    fun `crafted body is not recognized and never throws`() {
        val parser = ParserTestMessages.allParsers().single { it.institutionName == institutionName }

        assertNull(case, parser.parse(parser.senderPatterns.first(), body, receivedAt = 0L))
    }

    companion object {
        private const val BKASH = "bKash"
        private const val DBBL = "Dutch-Bangla Bank"
        private const val NAGAD = "Nagad"
        private const val UTTARA = "Uttara Bank"
        private const val PUBALI = "Pubali Bank"
        private const val BRAC = "BRAC Bank"

        private const val DBBL_STATEMENT_TAIL =
            " as on 30/06/25. To download statement click https://app.dutchbanglabank.com/cbsstatement . For query call 16216"
        private const val UTTARA_CREDIT_TAIL =
            " has been credited to your A/C No 032300000000001 from Sample Road Branch and Current Balance is: BDT -1452200"
        private const val PUBALI_CREDIT_TAIL =
            " has been credited to your account *** 865 through SAMPLE BAZAR, CITY branch on 20-08-2023. A/C Bal: Dr 334050.05"
        private const val BRAC_WITHDRAWAL_TAIL =
            " has been withdrawn from your A/C#130320**2001 on 22-03-22. Your A/C balance is TK 29,630.27. For Enquiry call: 16221"

        private fun case(institutionName: String, case: String, body: String) = arrayOf(institutionName, case, body)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1}")
        fun cases(): List<Array<String>> = listOf(
            case(BKASH, "lone comma amount, send money", "Send Money Tk , to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"),
            case(BKASH, "lone comma balance, money received", "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk ,. TrxID CCCCCCCCCC at 01/12/2024 10:00"),
            case(BKASH, "no digits after currency marker, cash in", "Cash In Tk  from 01700000008 successful. Fee Tk 0.00. Balance Tk 1,500.00. TrxID GGGGGGGGGG at 01/11/2025 10:00"),
            case(BKASH, "multiple dots in amount, payment", "Payment Tk 1.2.3 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"),
            case(BKASH, "comma and dots as balance, payment", "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk ,.. TrxID HHHHHHHHHH at 01/07/2026 10:00"),
            case(BKASH, "digits replaced by separators in amount, bank deposit", "You have received a deposit of Tk ,,,,,,,, from Sonali Bank Account. Fee Tk 0.00. Balance Tk 1,100.00. TrxID FFFFFFFFFF at 01/07/2025 10:00"),
            case(BKASH, "digits replaced by separators in balance, cash in", "Cash In Tk 1,000.00 from 01700000008 successful. Fee Tk 0.00. Balance Tk ,,,,,,,,. TrxID GGGGGGGGGG at 01/11/2025 10:00"),
            case(BKASH, "truncated after currency marker", "Send Money Tk"),
            case(BKASH, "truncated inside balance", "Send Money Tk 500.00 to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,"),

            case(DBBL, "lone comma amount, balance inquiry", "Balance of your A/C:***123 is BDT ,$DBBL_STATEMENT_TAIL"),
            case(DBBL, "lone comma amount, ATM transfer credit", "Dear Sir, your A/C ***123 credited (ATM A/C to A/C Transfer Credit) by Tk, on 01-02-2025 02:55:44 PM C/B Tk50,450.00. NexusPay https://bit.ly/nexuspay"),
            case(DBBL, "lone comma balance, ATM transfer credit", "Dear Sir, your A/C ***123 credited (ATM A/C to A/C Transfer Credit) by Tk50,000.00 on 01-02-2025 02:55:44 PM C/B Tk,. NexusPay https://bit.ly/nexuspay"),
            case(DBBL, "lone comma amount, cash out", "Cash-Out to A/C:***456Tk,Fee:Tk15.00 Your A/C Balance: Tk300.00.TxnId:1111111111Date:27-SEP-25 05:59:38 pm. Please download https://bit.ly/nexuspay"),
            case(DBBL, "lone comma balance, cash out", "Cash-Out to A/C:***456Tk900.00Fee:Tk15.00 Your A/C Balance: Tk,.TxnId:1111111111Date:27-SEP-25 05:59:38 pm. Please download https://bit.ly/nexuspay"),
            case(DBBL, "no digits after currency marker, balance inquiry", "Balance of your A/C:***123 is BDT $DBBL_STATEMENT_TAIL"),
            case(DBBL, "multiple dots in amount, balance inquiry", "Balance of your A/C:***123 is BDT 45..00$DBBL_STATEMENT_TAIL"),
            case(DBBL, "digits replaced by separators, balance inquiry", "Balance of your A/C:***123 is BDT ,,,,,,,,,$DBBL_STATEMENT_TAIL"),
            case(DBBL, "truncated after currency marker, balance inquiry", "Balance of your A/C:***123 is BDT"),
            case(DBBL, "truncated after closing balance marker, ATM transfer credit", "Dear Sir, your A/C ***123 credited (ATM A/C to A/C Transfer Credit) by Tk50,000.00 on 01-02-2025 02:55:44 PM C/B Tk"),

            case(NAGAD, "lone comma amount, cash in", "Cash In Received.\nAmount: Tk ,\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"),
            case(NAGAD, "lone comma balance, cash in", "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: ,\n11/01/2024 16:47"),
            case(NAGAD, "lone comma amount, money received", "Money Received.\nAmount: Tk ,\nSender: 01700000003\nRef: N/A\nTxnID: 73CCCCCC\nBalance: Tk 20300.32\n23/01/2025 20:58"),
            case(NAGAD, "lone comma balance, payment", "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk ,\n03/11/2023 13:58"),
            case(NAGAD, "no digits after currency marker, cash in", "Cash In Received.\nAmount: Tk \nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"),
            case(NAGAD, "multiple dots in amount, cash in", "Cash In Received.\nAmount: Tk 50..00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"),
            case(NAGAD, "digits replaced by separators in amount, cash in", "Cash In Received.\nAmount: Tk ,,,,,,,\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"),
            case(NAGAD, "digits replaced by separators in balance, payment", "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk ,,.,,\n03/11/2023 13:58"),
            case(NAGAD, "truncated after balance label, cash in", "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance:"),

            case(UTTARA, "lone comma amount, credit", "Dear Customer, BDT ,$UTTARA_CREDIT_TAIL"),
            case(UTTARA, "lone comma balance, debit", "Dear Customer, BDT 200000 has been debited from your A/C No 032300000000002 from Sample Branch and Current Balance is: BDT ,"),
            case(UTTARA, "minus and comma as balance, debit", "Dear Customer, BDT 200000 has been debited from your A/C No 032300000000002 from Sample Branch and Current Balance is: BDT -,"),
            case(UTTARA, "lone comma amount, balance statement", "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. , . For details, please contact your Branch."),
            case(UTTARA, "no digits after currency marker, credit", "Dear Customer, BDT $UTTARA_CREDIT_TAIL"),
            case(UTTARA, "multiple dots in amount, credit", "Dear Customer, BDT 478..00$UTTARA_CREDIT_TAIL"),
            case(UTTARA, "digits replaced by separators in amount, credit", "Dear Customer, BDT ,,,,,$UTTARA_CREDIT_TAIL"),
            case(UTTARA, "digits replaced by separators, balance statement", "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. ,,,,,.,, . For details, please contact your Branch."),
            case(UTTARA, "truncated after currency marker", "Dear Customer, BDT"),

            case(PUBALI, "lone comma amount, branch credit", "BDT ,$PUBALI_CREDIT_TAIL"),
            case(PUBALI, "lone comma balance after Dr, branch debit", "BDT 50000 has been debited from your account ***865 through SAMPLE BAZAR branch on 24-08-2023. A/C Bal: Dr ,"),
            case(PUBALI, "minus and comma as balance, EFT credit", "BDT 4374.78 has been credited to your a/c ***2865 through EFT SAMPLE BANK LIMITE,REMITTANCE,SAMPLEBANKLTD on 14-NOV-23. A/C Bal:-,"),
            case(PUBALI, "lone comma amount, PI Banking credit", "BDT , has been credited to your a/c ***123 from SAMPLE PERSON on 05-SEP-24 through PI Banking.A/C Bal:141500.00"),
            case(PUBALI, "lone comma balance, PI Banking credit", "BDT 2500.00 has been credited to your a/c ***123 from SAMPLE PERSON on 05-SEP-24 through PI Banking.A/C Bal:,"),
            case(PUBALI, "no digits after currency marker, branch credit", "BDT $PUBALI_CREDIT_TAIL"),
            case(PUBALI, "multiple dots in amount, branch credit", "BDT 14..00$PUBALI_CREDIT_TAIL"),
            case(PUBALI, "digits replaced by separators in amount, fund transfer", "BDT ,,,,,,,, has been credited to your account using FUND TRANSFER from SAMPLE COMMERCIAL BANK LIMITED on 24-DEC-23. A/C Bal:Dr 330365.72."),
            case(PUBALI, "truncated after balance label, branch credit", "BDT 140000 has been credited to your account *** 865 through SAMPLE BAZAR, CITY branch on 20-08-2023. A/C Bal:"),

            case(BRAC, "lone comma amount, withdrawal", "Tk ,$BRAC_WITHDRAWAL_TAIL"),
            case(BRAC, "lone comma balance, deposit", "Tk 38,740 has been deposited to your A/C#130320**2001 on 23-03-22. Your A/C balance is TK ,. For Enquiry call: 16221"),
            case(BRAC, "lone comma amount, cash deposit", "CASH DEPOSIT of TK , to your account 1303***2001 was successful on 07/05/23 3:30 pm. Your new balance is TK 46,743.77 . Helpline 16221"),
            case(BRAC, "lone comma amount, cheque clearing", "Dear Customer, clearing cheque No. 6525300 of BDT , has been deposited to A/C#130320**2001.Your A/C balance is TK 70,130.27.Enquiry: 16221."),
            case(BRAC, "lone comma balance, cheque clearing", "Dear Customer, clearing cheque No. 6525300 of BDT 26,600.00 has been deposited to A/C#130320**2001.Your A/C balance is TK ,.Enquiry: 16221."),
            case(BRAC, "no digits after currency marker, withdrawal", "Tk $BRAC_WITHDRAWAL_TAIL"),
            case(BRAC, "multiple dots in amount, withdrawal", "Tk 40..500$BRAC_WITHDRAWAL_TAIL"),
            case(BRAC, "digits replaced by separators in amount, RTGS debit", "TK ,,,,,,,,,, (incl. charges) has been debited from your A/C 130**2001 through RTGS on 08-05-23. Available balance: TK 20,000.00. Helpline: 16221"),
            case(BRAC, "truncated after balance currency marker, withdrawal", "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22. Your A/C balance is TK")
        )
    }
}
