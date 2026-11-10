package com.sajib.smsexpensetracker.importer

/**
 * Full flavor only. Reads the existing SMS inbox once into the transaction
 * store. Kept as an interface so live capture setup can be unit tested
 * without a real SMS provider.
 */
interface InboxImporter {

    /** Ingests every inbox SMS. Requires READ_SMS to be granted already. */
    suspend fun importInbox()
}
