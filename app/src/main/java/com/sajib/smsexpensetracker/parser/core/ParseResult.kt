package com.sajib.smsexpensetracker.parser.core

import java.math.BigDecimal

/**
 * The structured outcome of successfully parsing a transaction SMS.
 *
 * A parser returns null instead of this when the SMS is not a transaction
 * notification (for example, a promotional message from the same sender).
 *
 * Amounts use BigDecimal, never Double or Float, since this is currency and
 * must never be subject to floating point rounding error.
 */
data class ParseResult(
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val balance: BigDecimal?,
    val counterparty: String?,
    val reference: String?,
    val rawSms: String
)
