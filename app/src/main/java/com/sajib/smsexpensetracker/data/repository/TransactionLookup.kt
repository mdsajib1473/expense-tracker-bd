package com.sajib.smsexpensetracker.data.repository

import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Read-only checks against the stored transactions, as seen by the manual
 * intake use cases. Kept as an interface so they can be unit tested with a
 * fake.
 */
interface TransactionLookup {

    /**
     * True if a stored transaction has the same [institutionName], [type],
     * [amount] and [balance], whatever its date. A null [balance] matches
     * only a stored null balance.
     */
    suspend fun hasSimilar(
        institutionName: String,
        type: TransactionType,
        amount: BigDecimal,
        balance: BigDecimal?
    ): Boolean
}
