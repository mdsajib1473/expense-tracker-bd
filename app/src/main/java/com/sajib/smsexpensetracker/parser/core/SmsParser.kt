package com.sajib.smsexpensetracker.parser.core

/**
 * Contract every institution parser must implement.
 *
 * Implementations must be pure: given the same [sender], [body], and
 * [receivedAt], parse must always return the same result, with no database
 * writes, no network calls, and no UI updates. This keeps every parser
 * testable without an Android runtime and keeps the plugin boundary in
 * parser/institutions/ safe to extend without touching other parsers.
 */
interface SmsParser {

    /** Human-readable name shown in Settings > Parsers, for example "bKash". */
    val institutionName: String

    /** Sender addresses this parser handles, for example "bKash", "01678600000". */
    val senderPatterns: List<String>

    /**
     * Attempt to parse [body] sent by [sender] at [receivedAt] (epoch millis).
     * Returns null if this SMS is not a transaction notification.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParseResult?
}
