package com.sajib.smsexpensetracker.parser.core

/**
 * Routes an incoming SMS to the first registered parser whose sender
 * patterns match, and returns its result.
 *
 * At M0 no parsers are registered yet, so parse always returns null. This is
 * intentional: the milestone's job is to prove the engine shape compiles and
 * is testable, not to parse anything real yet. Real parsers get added to the
 * [parsers] list starting at M1, wired through Hilt in di/ParserModule.kt.
 *
 * SmsReceiver and the historical-import job must both go through this class;
 * neither should call an institution parser directly.
 */
class ParserEngine(
    private val parsers: List<SmsParser> = emptyList()
) {

    /**
     * Attempts to parse [body] from [sender] received at [receivedAt].
     * Returns the first non-null result from a matching parser, or null if
     * no registered parser's sender patterns match, or none is registered.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
        val matchingParser = parsers.firstOrNull { parser ->
            parser.senderPatterns.any { pattern ->
                sender.contains(pattern, ignoreCase = true)
            }
        } ?: return null

        return matchingParser.parse(sender, body, receivedAt)
    }
}
