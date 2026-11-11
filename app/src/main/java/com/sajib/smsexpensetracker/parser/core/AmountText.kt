package com.sajib.smsexpensetracker.parser.core

import java.math.BigDecimal

/** Thousands separator that banks and wallets print inside amounts, as in "4,495.00". */
private const val GROUPING_SEPARATOR = ","

/**
 * Converts amount or balance text that a parser pattern captured, such as
 * "4,495.00" or "-1452200", to a BigDecimal after dropping the grouping
 * commas. Money is BigDecimal only, never Double or Float.
 *
 * Returns null when what remains is not a number, for example when the
 * pattern matched a lone comma. SMS text is untrusted input, so a parser
 * must then answer "not recognized" (null) for the whole message instead of
 * throwing. For every text that is a number the result is exactly what
 * BigDecimal's own constructor gives, scale included.
 */
fun parseAmountOrNull(raw: String): BigDecimal? =
    try {
        BigDecimal(raw.replace(GROUPING_SEPARATOR, ""))
    } catch (e: NumberFormatException) {
        null
    }
