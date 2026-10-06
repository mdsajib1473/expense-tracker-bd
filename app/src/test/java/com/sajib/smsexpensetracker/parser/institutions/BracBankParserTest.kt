package com.sajib.smsexpensetracker.parser.institutions

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Body strings follow the real BRAC Bank wording from sender "BRAC BANK",
 * but every account mask, amount, cheque number, OTP and branch name is
 * invented.
 */
class BracBankParserTest {

    private val parser = BracBankParser()

    @Test
    fun `withdrawal is parsed as debit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Tk 40,500 has been withdrawn from your A/C#130320**2001 on 22-03-22. Your A/C balance is TK 29,630.27. For Enquiry call: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("40500"), result?.amount)
        assertEquals(BigDecimal("29630.27"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `deposit is parsed as credit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Tk 38,740 has been deposited to your A/C#130320**2001 on 23-03-22. Your A/C balance is TK 68,370.27. For Enquiry call: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("38740"), result?.amount)
        assertEquals(BigDecimal("68370.27"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `credit is parsed as credit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Tk 38,750 has been credited to your A/C#130320**2001 on 25-08-22. Your A/C balance is TK 56,728.77. For Enquiry call: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("38750"), result?.amount)
        assertEquals(BigDecimal("56728.77"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `debit with lowercase enquiry is parsed as debit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Tk 38,735 has been debited from your A/C#130320**2001 on 25-09-22. Your A/C balance is TK 8,008.77. For enquiry call: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("38735"), result?.amount)
        assertEquals(BigDecimal("8008.77"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch deposit with time and shorter mask is parsed as credit with branch`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 28,750.00 has been deposited to A/C#13032**2001 on 22-09-22, 11:21 AM at SAMPLE SME BRANCH. Balance TK 46,743.77. Query: 16221.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("28750.00"), result?.amount)
        assertEquals(BigDecimal("46743.77"), result?.balance)
        assertEquals("SAMPLE SME BRANCH", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch withdrawal with time is parsed as debit with branch`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 50,000.00 has been withdrawn from A/C#13032**2001 on 19-12-22, 12:17 PM at SAMPLE SME BRANCH. Balance TK 7,793.77. Query: 16221.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("50000.00"), result?.amount)
        assertEquals(BigDecimal("7793.77"), result?.balance)
        assertEquals("SAMPLE SME BRANCH", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `cheque clearing credit is parsed with cheque number as reference`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Customer, clearing cheque No. 6525300 of BDT 26,600.00 has been deposited to A/C#130320**2001.Your A/C balance is TK 70,130.27.Enquiry: 16221.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("26600.00"), result?.amount)
        assertEquals(BigDecimal("70130.27"), result?.balance)
        assertNull(result?.counterparty)
        assertEquals("6525300", result?.reference)
    }

    @Test
    fun `loan installment reminder is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Client: Please deposit your BRAC Bank SME loan installment of Tk 38,735.0 by 25th Mar. Kindly ignore if you have already made the deposit. Query-16221",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `loan installment paid confirmation is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Client: Your BRAC Bank SME loan installment of Tk 38,735 has been paid successfully. Thanks for the payment. Enquiry: 16221",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `installment paid with overdue notice is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Client: Your installment of Tk 18,536.48 has been paid. Your overdue is TK 20,198.52. Kindly pay the due at your earliest convenience. Enquiry: 16221",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `account deposit otp message is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Use OTP 000000 to proceed with Account Deposit/Credit Card bill payment. This OTP will be valid for 3 minutes. Please do not share it with anyone.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `credit with space after A-C hash is parsed as credit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 38,750 has been credited to your A/C# 130320**2001 on 25-08-22. Your A/C balance is TK 56,728.77. For Enquiry call: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("38750"), result?.amount)
        assertEquals(BigDecimal("56728.77"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch deposit with BRAC Bank prefix is parsed as credit with branch`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 12,500.00 has been deposited to BRAC Bank A/C#13032**2001 on 03-05-23, 11:21 AM at SAMPLE SME/KRISHI BRANCH. Balance TK 46,743.77. Query: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("12500.00"), result?.amount)
        assertEquals(BigDecimal("46743.77"), result?.balance)
        assertEquals("SAMPLE SME/KRISHI BRANCH", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `branch withdrawal with BRAC Bank prefix is parsed as debit with branch`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 9,000.00 has been withdrawn from BRAC Bank A/C#13032**2001 on 04-05-23, 02:10 PM at SAMPLE SME/KRISHI BRANCH. Balance TK 37,743.77. Query: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("9000.00"), result?.amount)
        assertEquals(BigDecimal("37743.77"), result?.balance)
        assertEquals("SAMPLE SME/KRISHI BRANCH", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `incoming transfer from other bank is parsed as credit with source`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 5,000.00 credited to A/C#13032**2001 on 06-05-23 @10:15 AM from OTHER BANK. Balance TK 42,743.77. BRAC Bank.",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("5000.00"), result?.amount)
        assertEquals(BigDecimal("42743.77"), result?.balance)
        assertEquals("OTHER BANK", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `cash deposit is parsed as credit`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "CASH DEPOSIT of TK 4,000.00 to your account 1303***2001 was successful on 07/05/23 3:30 pm. Your new balance is TK 46,743.77 . Helpline 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("4000.00"), result?.amount)
        assertEquals(BigDecimal("46743.77"), result?.balance)
        assertNull(result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `rtgs debit including charges is parsed as debit with channel`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 100,500.00 (incl. charges) has been debited from your A/C 130**2001 through RTGS on 08-05-23. Available balance: TK 20,000.00. Helpline: 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(BigDecimal("100500.00"), result?.amount)
        assertEquals(BigDecimal("20000.00"), result?.balance)
        assertEquals("RTGS", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `loan linked account deposit is parsed as credit with outlet`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "TK 30,000.00 deposited to your loan-linked A/C 1303***2001 on 09/05/23 4:05 pm at AB Outlet. Your balance is TK 50,000.00. Helpline 16221",
            receivedAt = 0L
        )

        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(BigDecimal("30000.00"), result?.amount)
        assertEquals(BigDecimal("50000.00"), result?.balance)
        assertEquals("AB Outlet", result?.counterparty)
        assertNull(result?.reference)
    }

    @Test
    fun `term loan instalment paid is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Client, your BRAC Bank SME Term Loan instalment of BDT 1,234,567.89 has been paid successfully. Thanks for the payment. Enquiry: 16221",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `term loan disbursement is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Client, your SME Term Loan of TK 1,000,000.00 has been disbursed. Your instalment TK 40,000.00 is payable on 25 day of each month. Enquiry: 16221",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `dormancy prevention notice is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Customer,to keep your account active,please make debit transactions of any amount from your AC13**01 by 30-06-23. Call 16221 or Visit nearest branch",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `self registration otp is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Use OTP: 000000 to proceed with Self registration. This OTP will be valid for 5 minutes. Please do not share it with anyone.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `cheque book request notice is never parsed`() {
        val result = parser.parse(
            sender = "BRAC BANK",
            body = "Dear Customer, Your Cheque Book request has been received from Call Center. For query, use Ref# 000. For details Call 16221.",
            receivedAt = 0L
        )

        assertNull(result)
    }

    @Test
    fun `sender patterns contain only the spaced address and never a bare BRAC prefix`() {
        assertEquals(listOf("BRAC BANK"), parser.senderPatterns)
    }
}
