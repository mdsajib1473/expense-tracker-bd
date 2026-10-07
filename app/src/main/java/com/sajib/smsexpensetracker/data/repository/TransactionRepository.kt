package com.sajib.smsexpensetracker.data.repository

import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.data.db.TransactionDao
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Single source of truth for transaction data. Writes arrive only through
 * the ingestion entry point (IngestSmsUseCase), never TransactionDao directly.
 */
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) : TransactionWriter {

    /**
     * Inserts [parsed] unless a row with the same institution, SMS timestamp,
     * amount, type and balance already exists. The check and the insert run
     * in one database transaction.
     */
    override suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean {
        val result = parsed.result
        return dao.insertIfAbsent(
            Transaction(
                institutionName = parsed.institutionName,
                type = result.type,
                amount = result.amount,
                currency = result.currency,
                balance = result.balance,
                counterparty = result.counterparty,
                reference = result.reference,
                rawSms = result.rawSms,
                receivedAt = receivedAt,
                insertedAt = System.currentTimeMillis()
            )
        )
    }

    /** Every stored transaction, newest first. */
    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    /** Transactions whose SMS arrived between [start] and [end] inclusive, newest first. */
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> =
        dao.getByDateRange(start, end)
}
