package com.sajib.smsexpensetracker.di

import android.content.Context
import androidx.room.Room
import com.sajib.smsexpensetracker.data.db.AppDatabase
import com.sajib.smsexpensetracker.data.db.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and its DAOs. No destructive migration fallback
 * is configured here, per AGENT.md Hard Constraint 5: every future schema
 * change ships an explicit Migration.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** The single on-device database instance. */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sms_expense_tracker.db")
            .build()

    /** DAO for the transactions table, backed by the singleton database. */
    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
}
