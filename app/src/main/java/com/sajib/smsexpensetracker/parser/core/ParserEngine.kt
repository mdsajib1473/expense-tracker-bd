package com.sajib.smsexpensetracker.parser.core

/**
 * Routes an incoming SMS to the first registered parser whose sender
 * patterns match, and returns its result wrapped with the institution name.
 *
 * SmsReceiver and the historical-import job must both go through this class;
 * neither should call an institution parser directly.
 */
class ParserEngine(
    private val parsers: List<SmsParser> = emptyList()
) {

    /**
     * Attempts to parse [body] from [sender] received at [receivedAt].
     * Returns null if no registered parser's sender patterns match, or if
     * the matching parser itself returns null (not a transaction SMS).
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        val matchingParser = parsers.firstOrNull { parser ->
            parser.senderPatterns.any { pattern ->
                sender.contains(pattern, ignoreCase = true)
            }
        } ?: return null

        val result = matchingParser.parse(sender, body, receivedAt) ?: return null
        return ParsedTransaction(matchingParser.institutionName, result)
    }
}
