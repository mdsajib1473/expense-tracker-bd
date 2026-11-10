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

    /**
     * Identifies a message that arrived without a sender address (shared or
     * pasted text) from its [body] alone. Every registered parser is run on
     * the body, each with the first of its own declared [SmsParser.senderPatterns]
     * as the sender, which is exactly the call the sender-routed [parse]
     * makes for a message from that sender, so every parser applies its own
     * body rules unchanged. A parser with no sender pattern can never be
     * routed to by [parse] either and is skipped.
     *
     * The result is accepted only when exactly one parser recognizes the
     * body; two or more give [BodyIdentification.Ambiguous]. A parser that
     * fails with a [NumberFormatException] on this body (its pattern matched
     * but a captured amount is not a number) counts as not recognizing it.
     */
    fun identifyByBody(body: String, receivedAt: Long): BodyIdentification {
        val matches = parsers.mapNotNull { parser ->
            val ownSender = parser.senderPatterns.firstOrNull() ?: return@mapNotNull null
            val result = try {
                parser.parse(ownSender, body, receivedAt)
            } catch (e: NumberFormatException) {
                null
            }
            result?.let { ParsedTransaction(parser.institutionName, it) }
        }
        val single = matches.singleOrNull()
        return when {
            single != null -> BodyIdentification.Matched(single.institutionName, single.result)
            matches.isEmpty() -> BodyIdentification.NoMatch
            else -> BodyIdentification.Ambiguous
        }
    }
}
