package com.sajib.smsexpensetracker.di

import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the write-side interface used by the ingestion entry point to the
 * Room-backed repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** TransactionRepository is the only production TransactionWriter. */
    @Binds
    abstract fun bindTransactionWriter(repository: TransactionRepository): TransactionWriter
}
