package com.sajib.smsexpensetracker.ui.common

import androidx.annotation.StringRes
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.parser.core.TransactionType

/** The string resource naming this transaction type to the user. */
@StringRes
fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.DEBIT -> R.string.transaction_type_debit
    TransactionType.CREDIT -> R.string.transaction_type_credit
    TransactionType.BALANCE_INQUIRY -> R.string.transaction_type_balance_inquiry
    TransactionType.RECHARGE -> R.string.transaction_type_recharge
    TransactionType.AIRTIME -> R.string.transaction_type_airtime
}
