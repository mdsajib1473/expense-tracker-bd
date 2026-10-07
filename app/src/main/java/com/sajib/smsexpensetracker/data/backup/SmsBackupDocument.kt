package com.sajib.smsexpensetracker.data.backup

/**
 * An opened backup file: the message count its root declares, if any, and
 * its entries in file order. [entries] is lazy and can be iterated once;
 * iterating it may throw a BackupImportException.
 */
class SmsBackupDocument(
    val countHint: Int?,
    val entries: Sequence<SmsBackupEntry>
)
