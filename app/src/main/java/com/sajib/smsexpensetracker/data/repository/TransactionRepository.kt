package com.sajib.smsexpensetracker.data.repository

import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.data.db.TransactionDao
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Single source of truth for transaction data. SmsReceiver and the future
 * import job must go through this, never TransactionDao directly.
 */
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {

    suspend fun save(parsed: ParsedTransaction, receivedAt: Long) {
        val result = parsed.result
        dao.insert(
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

    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> =
        dao.getByDateRange(start, end)
}
