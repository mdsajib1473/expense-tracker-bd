package com.sajib.smsexpensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Data access for the transactions table. De-duplication is a query, not a
 * unique index, so it needs no schema change or migration.
 */
@Dao
interface TransactionDao {

    /** Inserts [transaction] unconditionally and returns its new row id. */
    @Insert
    suspend fun insert(transaction: Transaction): Long

    /**
     * True if a row with the same institution, SMS timestamp, amount, type and
     * balance exists. "IS" makes a null balance match a stored null balance.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM transactions WHERE institutionName = :institutionName " +
            "AND receivedAt = :receivedAt AND amount = :amount AND type = :type AND balance IS :balance)"
    )
    suspend fun exists(
        institutionName: String,
        receivedAt: Long,
        amount: BigDecimal,
        type: TransactionType,
        balance: BigDecimal?
    ): Boolean

    /**
     * Inserts [transaction] only if no identical row exists (see [exists]),
     * inside one database transaction. Returns true if a row was written.
     */
    @androidx.room.Transaction
    suspend fun insertIfAbsent(transaction: Transaction): Boolean {
        val duplicate = exists(
            transaction.institutionName,
            transaction.receivedAt,
            transaction.amount,
            transaction.type,
            transaction.balance
        )
        if (duplicate) return false
        insert(transaction)
        return true
    }

    /** Every stored transaction, newest first. */
    @Query("SELECT * FROM transactions ORDER BY receivedAt DESC")
    fun getAll(): Flow<List<Transaction>>

    /** Transactions whose SMS arrived between [start] and [end] inclusive, newest first. */
    @Query("SELECT * FROM transactions WHERE receivedAt BETWEEN :start AND :end ORDER BY receivedAt DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>>
}
