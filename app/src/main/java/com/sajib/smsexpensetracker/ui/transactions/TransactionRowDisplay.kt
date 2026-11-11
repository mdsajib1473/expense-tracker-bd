package com.sajib.smsexpensetracker.ui.transactions

import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/** How one row of the transaction list presents its figure. */
internal enum class AmountKind {

    /** Money came in: shown with a plus sign. */
    MONEY_IN,

    /** Money went out: shown with a minus sign. */
    MONEY_OUT,

    /** No money moved, the message only reported a balance: shown with no sign of its own. */
    BALANCE
}

/** The kind of figure this transaction shows in the list. */
internal fun Transaction.amountKind(): AmountKind = when (type) {
    TransactionType.CREDIT -> AmountKind.MONEY_IN
    TransactionType.BALANCE_INQUIRY -> AmountKind.BALANCE
    TransactionType.DEBIT, TransactionType.RECHARGE, TransactionType.AIRTIME -> AmountKind.MONEY_OUT
}

/**
 * The figure this transaction shows in the list: the reported balance for a
 * balance inquiry, otherwise the amount moved. A balance that is itself
 * negative (an overdraft) keeps its minus, because that is its value.
 */
internal fun Transaction.displayedFigure(): BigDecimal =
    if (type == TransactionType.BALANCE_INQUIRY) balance ?: amount else amount
