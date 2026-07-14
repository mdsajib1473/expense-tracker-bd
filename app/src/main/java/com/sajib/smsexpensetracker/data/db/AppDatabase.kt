package com.sajib.smsexpensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 1: initial schema, transactions table only. Any future schema
 * change must ship an explicit Migration, per AGENT.md Hard Constraint 5,
 * fallbackToDestructiveMigration is never used outside debug builds.
 */
@Database(entities = [Transaction::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
