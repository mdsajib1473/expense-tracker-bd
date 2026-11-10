package com.sajib.smsexpensetracker.parser

import com.sajib.smsexpensetracker.parser.core.SmsParser
import com.sajib.smsexpensetracker.parser.institutions.BkashParser
import com.sajib.smsexpensetracker.parser.institutions.BracBankParser
import com.sajib.smsexpensetracker.parser.institutions.DutchBanglaParser
import com.sajib.smsexpensetracker.parser.institutions.NagadParser
import com.sajib.smsexpensetracker.parser.institutions.PubaliBankParser
import com.sajib.smsexpensetracker.parser.institutions.UttaraBankParser

/**
 * Shared test fixture: every message body used by the institution parser
 * tests (BkashParserTest, DutchBanglaParserTest, NagadParserTest,
 * BracBankParserTest, PubaliBankParserTest, UttaraBankParserTest), copied
 * verbatim, so other tests can run the same messages through the engine.
 * Every phone number, account, TrxID, OTP, name and amount is invented.
 */
internal object ParserTestMessages {

    /** A message body, the institution whose parser test uses it, and that institution's real sender. */
    data class Sample(val institutionName: String, val sender: String, val body: String)

    /** Every registered institution parser, in the order ParserModule lists them. */
    fun allParsers(): List<SmsParser> = listOf(
        BkashParser(),
        DutchBanglaParser(),
        NagadParser(),
        UttaraBankParser(),
        PubaliBankParser(),
        BracBankParser()
    )

    /** One body per success case in the parser tests; each parser recognizes its own. */
    val transactions: List<Sample> = listOf(
        Sample("bKash", "bKash", "Send Money Tk 500.00 to 01700000004 successful. Ref SAMPLE. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"),
        Sample("bKash", "bKash", "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00"),
        Sample("bKash", "bKash", "You have received Tk 20,000.00 from 01700000007.Ref sample note. Fee Tk 0.00. Balance Tk 20,150.00. TrxID DDDDDDDDDD at 01/01/2025 10:00"),
        Sample("bKash", "bKash", "You have received Tk 60.00 from SAMPLE MERCHANT. Balance Tk 1,060.00. TrxID EEEEEEEEEE at 01/05/2026 10:00"),
        Sample("bKash", "bKash", "You have received a deposit of Tk 1,000.00 from Sonali Bank Account. Fee Tk 0.00. Balance Tk 1,100.00. TrxID FFFFFFFFFF at 01/07/2025 10:00"),
        Sample("bKash", "bKash", "Cash In Tk 1,000.00 from 01700000008 successful. Fee Tk 0.00. Balance Tk 1,500.00. TrxID GGGGGGGGGG at 01/11/2025 10:00. Download App: https://bKa.sh/8app"),
        Sample("bKash", "bKash", "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"),
        Sample("bKash", "bKash", "Payment of Tk 1,000.50 to SAMPLE MERCHANT-RM00002 is successful. Balance Tk 1,100.00. TrxID JJJJJJJJJJ at 01/03/2026 10:00"),
        Sample("Dutch-Bangla Bank", "16216", "Balance of your A/C:***123 is BDT 45,000.00 as on 30/06/25. To download statement click https://app.dutchbanglabank.com/cbsstatement . For query call 16216"),
        Sample("Dutch-Bangla Bank", "16216", "Balance of your A/C:***123 is BDT 450.00 as on 31/12/24. To download statement click https://app.dutchbanglabank.com/cbsstatement . For query call 16216"),
        Sample("Dutch-Bangla Bank", "16216", "Dear Sir, your A/C ***123 credited (ATM A/C to A/C Transfer Credit) by Tk50,000.00 on 01-02-2025 02:55:44 PM C/B Tk50,450.00. NexusPay https://bit.ly/nexuspay"),
        Sample("Dutch-Bangla Bank", "16216", "Cash-Out to A/C:***456Tk900.00Fee:Tk15.00 Your A/C Balance: Tk300.00.TxnId:1111111111Date:27-SEP-25 05:59:38 pm. Please download https://bit.ly/nexuspay"),
        Sample("Nagad", "NAGAD", "Cash In Received.\nAmount: Tk 5000.00\nUddokta: 01700000001\nTxnID: 72AAAAAA\nBalance: 5032.90\n11/01/2024 16:47"),
        Sample("Nagad", "NAGAD", "WIN LAND IN DHAKA! CLICK NOW  nagad.io/xyz\nCash In Received.\nAmt:Tk 200.00\nUddokta:01700000002\nTxnID: 72BBBBBB\nBal:208.65\n27/03/2024 15:54"),
        Sample("Nagad", "NAGAD", "Money Received.\nAmount: Tk 20300.00\nSender: 01700000003\nRef: N/A\nTxnID: 73CCCCCC\nBalance: Tk 20300.32\n23/01/2025 20:58"),
        Sample("Nagad", "NAGAD", "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk 32.90\n03/11/2023 13:58"),
        Sample("BRAC Bank", "BRAC BANK", "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22. Your A/C balance is TK 29,630.27. For Enquiry call: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Tk 38,740 has been deposited to your A/C#130320**2001 on 23-03-22. Your A/C balance is TK 68,370.27. For Enquiry call: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Tk 38,750 has been credited to your A/C#130320**2001 on 25-08-22. Your A/C balance is TK 56,728.77. For Enquiry call: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Tk 38,735 has been debited from your A/C#130320**2001 on 25-09-22. Your A/C balance is TK 8,008.77. For enquiry call: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 28,750.00 has been deposited to A/C#13032**2001 on 22-09-22, 11:21 AM at SAMPLE SME BRANCH. Balance TK 46,743.77. Query: 16221."),
        Sample("BRAC Bank", "BRAC BANK", "TK 50,000.00 has been withdrawn from A/C#13032**2001 on 19-12-22, 12:17 PM at SAMPLE SME BRANCH. Balance TK 7,793.77. Query: 16221."),
        Sample("BRAC Bank", "BRAC BANK", "Dear Customer, clearing cheque No. 6525300 of BDT 26,600.00 has been deposited to A/C#130320**2001.Your A/C balance is TK 70,130.27.Enquiry: 16221."),
        Sample("BRAC Bank", "BRAC BANK", "TK 38,750 has been credited to your A/C# 130320**2001 on 25-08-22. Your A/C balance is TK 56,728.77. For Enquiry call: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 12,500.00 has been deposited to BRAC Bank A/C#13032**2001 on 03-05-23, 11:21 AM at SAMPLE SME/KRISHI BRANCH. Balance TK 46,743.77. Query: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 9,000.00 has been withdrawn from BRAC Bank A/C#13032**2001 on 04-05-23, 02:10 PM at SAMPLE SME/KRISHI BRANCH. Balance TK 37,743.77. Query: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 5,000.00 credited to A/C#13032**2001 on 06-05-23 @10:15 AM from OTHER BANK. Balance TK 42,743.77. BRAC Bank."),
        Sample("BRAC Bank", "BRAC BANK", "CASH DEPOSIT of TK 4,000.00 to your account 1303***2001 was successful on 07/05/23 3:30 pm. Your new balance is TK 46,743.77 . Helpline 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 100,500.00 (incl. charges) has been debited from your A/C 130**2001 through RTGS on 08-05-23. Available balance: TK 20,000.00. Helpline: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "TK 30,000.00 deposited to your loan-linked A/C 1303***2001 on 09/05/23 4:05 pm at AB Outlet. Your balance is TK 50,000.00. Helpline 16221"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 140000 has been credited to your account *** 865 through SAMPLE BAZAR, CITY branch on 20-08-2023. A/C Bal: Dr 334050.05"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 50000 has been debited from your account ***865 through SAMPLE BAZAR branch on 24-08-2023. A/C Bal: Dr 434061.55"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 4374.78 has been credited to your a/c ***2865 through EFT SAMPLE BANK LIMITE,REMITTANCE,SAMPLEBANKLTD on 14-NOV-23. A/C Bal:-283050.77"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 70000.00 has been credited to your account using FUND TRANSFER from SAMPLE COMMERCIAL BANK LIMITED on 24-DEC-23. A/C Bal:Dr 330365.72."),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 5000 has been debited from your account *** 123 through SAMPLE BAZAR, CITY branch on 01-02-2024. A/C Bal: Cr 120000.50"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 7000 has been credited to your account *** 123 through SAMPLE BAZAR, CITY branch on 02-02-2024. A/C Bal: Cr 127000"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 3000 has been debited from your account *** 123 through SAMPLE ISLAMIC BANKING branch  on 03-02-2024. A/C Bal: Cr 124000.00"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 15000.00 has been credited to your account using FUND TRANSFER from SAMPLE ISLAMI BANK LIMITED on 04-JUL-24. A/C Bal:Cr 139000.00."),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 2500.00 has been credited to your a/c ***123 through EFT SAMPLE BANK LIMITE,REMITTANCE,NOTAPPLICABLE on 05-APR-24. A/C Bal:141500.00"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 2500.00 has been credited to your a/c ***123 from SAMPLE PERSON on 05-SEP-24 through PI Banking.A/C Bal:141500.00"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 5000 has been debited from your account *** 123 through SAMPLE BAZAR, CITY branch on 01-02-2024. A/C Bal: Dr 120000.50"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 47800 has been credited to your A/C No 032300000000001 from Sample Road Branch and Current Balance is: BDT -1452200"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 200000 has been debited from your A/C No 032300000000002 from Sample Branch and Current Balance is: BDT -200258.75"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 14000  has been credited to your A/C No 032300000000003 from Head Office and Current Balance is: BDT 52000.50"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 1000 has been credited to your A/C No 032300000000003 from Local Office and Current Balance is: BDT -9000.00"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 3000.50 has been debited from your A/C No 0323****0003 for SMS Service Charge  fromSample Road Branch and Current Balance is:BDT 48999.50"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 700.25 has been credited to your A/C No 0323****0003   from Sample Road Branch and Current Balance is:BDT 5000.10"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, BDT 5000 has been credited to your A/C No 032300000000003 from Sample Road Branch"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. 12345.67 . For details, please contact your Branch."),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. 12345 . For details, please contact your Branch."),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Sir, balance of your A/C No 0323****0003 as on 1/2/2024 is Tk. -12345.67 . For Details, please contact your Branch.")
    )

    /** Every body a parser test expects its parser to reject (OTP, notices, promotions, unknown formats). */
    val nonTransactions: List<Sample> = listOf(
        Sample("bKash", "bKash", "Sorry, your Send Money request to 01700000005 is unsuccessful! Tk 30 has been returned. Balance Tk 70.00. TrxID BBBBBBBBBB at 01/11/2024 10:00"),
        Sample("bKash", "bKash", "Your bKash verification code is 000000. Expires in 2 minutes."),
        Sample("bKash", "bKash", "Some future bKash message format we have never seen before."),
        Sample("Dutch-Bangla Bank", "16216", "Dear Customer, 000000 is your OTP for e-com transaction of Debit Card no# 0000****0000 which will be valid for 5 minutes."),
        Sample("Dutch-Bangla Bank", "16216", "Some future DBBL message format we have never seen before."),
        Sample("Nagad", "NAGAD", "Your One Time Password (OTP) for Nagad ECOM is 000000. Validity for OTP is 10 minutes. Helpline 16167."),
        Sample("Nagad", "NAGAD", "Never Share Any Code.\nDevice registration request.\nUsername:01700000009\nOTP/Code: 000000\nExpiry time:09:51:32\nUID: AAAAAAAAAAA"),
        Sample("Nagad", "NAGAD", "সতর্কতা: আপনার পিন বা কোড কাউকে দেবেন না।"),
        Sample("Nagad", "NAGAD", "Congrats! You've received Cashback 5.0 Tk for Mobile Recharge of 100.0 Tk. | 01/02/2024 10:00 | For Details Call 16167"),
        Sample("BRAC Bank", "BRAC BANK", "Dear Client: Please deposit your BRAC Bank SME loan installment of Tk 38,735.0 by 25th Mar. Kindly ignore if you have already made the deposit. Query-16221"),
        Sample("BRAC Bank", "BRAC BANK", "Dear Client: Your BRAC Bank SME loan installment of Tk 38,735 has been paid successfully. Thanks for the payment. Enquiry: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Dear Client: Your installment of Tk 18,536.48 has been paid. Your overdue is TK 20,198.52. Kindly pay the due at your earliest convenience. Enquiry: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Use OTP 000000 to proceed with Account Deposit/Credit Card bill payment. This OTP will be valid for 3 minutes. Please do not share it with anyone."),
        Sample("BRAC Bank", "BRAC BANK", "Dear Client, your BRAC Bank SME Term Loan instalment of BDT 1,234,567.89 has been paid successfully. Thanks for the payment. Enquiry: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Dear Client, your SME Term Loan of TK 1,000,000.00 has been disbursed. Your instalment TK 40,000.00 is payable on 25 day of each month. Enquiry: 16221"),
        Sample("BRAC Bank", "BRAC BANK", "Dear Customer,to keep your account active,please make debit transactions of any amount from your AC13**01 by 30-06-23. Call 16221 or Visit nearest branch"),
        Sample("BRAC Bank", "BRAC BANK", "Use OTP: 000000 to proceed with Self registration. This OTP will be valid for 5 minutes. Please do not share it with anyone."),
        Sample("BRAC Bank", "BRAC BANK", "Dear Customer, Your Cheque Book request has been received from Call Center. For query, use Ref# 000. For details Call 16221."),
        Sample("Pubali Bank", "PUBALI BANK", "Please collect your Cheque Book (0000-000-00000) from SAMPLE Branch"),
        Sample("Pubali Bank", "PUBALI BANK", "BDT 450.00 RECEIVED FROM 01700****00 ,bKash USING QR PAYMENT ON 05-SEP-24 .TrxID:AAAAAAAAAA."),
        Sample("Pubali Bank", "PUBALI BANK", "Dear Merchant! TK 450.00 has been credited for Merchant-QR transaction On Settlement Date 06-SEP-24. Stay with Pubali Bank."),
        Sample("Pubali Bank", "PUBALI BANK", "Dear Customer, you are requested to use Pubali PI Banking App. Enjoy easy Fund Transfer, Bill Payment, Mobile Recharge and more at your Fingertips"),
        Sample("Pubali Bank", "PUBALI BANK", "Dear Customer, You are requested to visit your Branch for Bio-Metric Registration (Thumbs and Face) and enjoy Smart Banking."),
        Sample("Pubali Bank", "PUBALI BANK", "Dear Sir, A/C 0000-000-00000 will change to 0000-000-00001 from 01/01/24, due to branch merging."),
        Sample("Pubali Bank", "PUBALI BANK", "Dear Customer, thank you for banking with us"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Valued Customer\nYour sms banking account has been created, a/c no 0323-00000000001 and pin no is 0000\nFor Help: Type ubl help and send to 26969.Thanks"),
        Sample("Uttara Bank", "UTTARA BANK", "Your A/C:00000000001 Loan instalments will be due on 10-FEB-23. Please disregard if already paid. Thanks for banking with UTTARA BANK LTD"),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Sir, Your cheque book is ready for delivery, A/C No.: 0323-00000000002, Pls take delivery from Sample Branch."),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, your fund transfer through RTGS of TK. 100000 to PBL of A/C No. 000000000001 has been sent."),
        Sample("Uttara Bank", "UTTARA BANK", "Dear Customer, For any banking queries and support, please call our Contact Center Number 16000. Thank you for banking with us."),
        Sample("Uttara Bank", "UTTARA BANK", "Your A/C:00000000001 Loan instalments will be due on 10-DEC-24. Please disregard if already paid. Thanks for banking with UTTARA BANK PLC."),
        Sample("Uttara Bank", "UTTARA BANK", "অপরিচিত লিংকে ক্লিক করবেন না।")
    )
}
