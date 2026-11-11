package com.sajib.smsexpensetracker.ui.transactions

import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/** Covers what a transaction list row shows: sign by type, and the balance for a balance inquiry. */
class TransactionRowDisplayTest {

    private fun transaction(type: TransactionType, amount: String, balance: String?) = Transaction(
        institutionName = "Sample Bank",
        type = type,
        amount = BigDecimal(amount),
        currency = "BDT",
        balance = balance?.let(::BigDecimal),
        counterparty = null,
        reference = null,
        rawSms = "",
        receivedAt = 0L,
        insertedAt = 0L
    )

    @Test
    fun `credit is money in and every other money movement is money out`() {
        assertEquals(AmountKind.MONEY_IN, transaction(TransactionType.CREDIT, "10.00", "20.00").amountKind())
        listOf(TransactionType.DEBIT, TransactionType.RECHARGE, TransactionType.AIRTIME).forEach { type ->
            assertEquals(type.name, AmountKind.MONEY_OUT, transaction(type, "10.00", "20.00").amountKind())
        }
    }

    @Test
    fun `balance inquiry is neither money in nor money out`() {
        assertEquals(AmountKind.BALANCE, transaction(TransactionType.BALANCE_INQUIRY, "45000.00", "45000.00").amountKind())
    }

    @Test
    fun `balance inquiry shows its balance and money movements show their amount`() {
        assertEquals(BigDecimal("45000.00"), transaction(TransactionType.BALANCE_INQUIRY, "1.00", "45000.00").displayedFigure())
        assertEquals(BigDecimal("10.00"), transaction(TransactionType.DEBIT, "10.00", "20.00").displayedFigure())
        assertEquals(BigDecimal("10.00"), transaction(TransactionType.CREDIT, "10.00", null).displayedFigure())
    }

    @Test
    fun `balance inquiry with no stored balance falls back to its amount`() {
        assertEquals(BigDecimal("450.00"), transaction(TransactionType.BALANCE_INQUIRY, "450.00", null).displayedFigure())
    }

    @Test
    fun `an overdrawn balance keeps its own minus`() {
        val figure = transaction(TransactionType.BALANCE_INQUIRY, "-12345.67", "-12345.67").displayedFigure()

        assertEquals("-12345.67", figure.toPlainString())
    }
}
