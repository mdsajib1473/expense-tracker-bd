package com.sajib.smsexpensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Query("SELECT * FROM transactions ORDER BY receivedAt DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE receivedAt BETWEEN :start AND :end ORDER BY receivedAt DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>>
}
