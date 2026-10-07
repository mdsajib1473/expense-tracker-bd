package com.sajib.smsexpensetracker.domain.model

/**
 * Counts from one backup import. Holds counts only, never a body, a sender
 * address or an institution name, so it can be logged and shown freely.
 *
 * Every message read lands in exactly one bucket:
 * totalRead = saved + duplicates + unrecognized + skippedNotInbox + invalid.
 */
data class ImportSummary(
    /** Messages read from the file, of any type. */
    val totalRead: Int,
    /** Recognized and written as new transactions. */
    val saved: Int,
    /** Recognized, but an identical transaction was already stored. */
    val duplicates: Int,
    /** Inbox messages no parser recognized. Dropped, never stored. */
    val unrecognized: Int,
    /** Sent, draft or other non-inbox messages, and non-SMS entries such as MMS. Never parsed. */
    val skippedNotInbox: Int,
    /** Inbox messages missing a sender, a body or a valid date. Never parsed. */
    val invalid: Int
)
