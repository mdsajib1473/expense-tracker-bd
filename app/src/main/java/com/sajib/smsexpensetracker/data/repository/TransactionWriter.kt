package com.sajib.smsexpensetracker.data.repository

import com.sajib.smsexpensetracker.parser.core.ParsedTransaction

/**
 * Write side of the transaction store, as seen by the ingestion entry point.
 * Kept as an interface so the entry point can be unit tested with a fake.
 */
interface TransactionWriter {

    /**
     * Stores [parsed], received at [receivedAt], unless an identical
     * transaction is already stored. Returns true if a row was written.
     */
    suspend fun saveIfNew(parsed: ParsedTransaction, receivedAt: Long): Boolean
}
