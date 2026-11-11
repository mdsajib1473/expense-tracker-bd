package com.sajib.smsexpensetracker.parser.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Routes an incoming SMS to the first registered parser whose sender
 * patterns match, and returns its result wrapped with the institution name.
 *
 * SmsReceiver and the historical-import job must both go through this class;
 * neither should call an institution parser directly.
 *
 * Both entry points are a defensive boundary around the parsers. SMS text is
 * untrusted input, so an exception thrown by a parser is treated as that
 * parser not recognizing the message, exactly as if it had returned null,
 * and one failing parser can never crash live capture, an import or the
 * share and paste intake. Nothing is logged and the exception never reaches
 * the caller, because its message can hold message text. Parsers are still
 * expected not to throw; this guard is the last line of defence, not a
 * substitute for a parser handling bad text itself.
 */
class ParserEngine(
    private val parsers: List<SmsParser> = emptyList()
) {

    /**
     * Attempts to parse [body] from [sender] received at [receivedAt].
     * Returns null if no registered parser's sender patterns match, or if
     * the matching parser itself returns null (not a transaction SMS) or
     * throws. Only the first parser whose sender pattern matches is asked;
     * the engine never falls through to another one.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        val matchingParser = parsers.firstOrNull { parser ->
            parser.senderPatterns.any { pattern ->
                sender.contains(pattern, ignoreCase = true)
            }
        } ?: return null

        val result = parseGuarded(matchingParser, sender, body, receivedAt) ?: return null
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
     * throws on this body counts as not recognizing it, and the others are
     * still asked.
     */
    fun identifyByBody(body: String, receivedAt: Long): BodyIdentification {
        val matches = parsers.mapNotNull { parser ->
            val ownSender = parser.senderPatterns.firstOrNull() ?: return@mapNotNull null
            parseGuarded(parser, ownSender, body, receivedAt)?.let { ParsedTransaction(parser.institutionName, it) }
        }
        val single = matches.singleOrNull()
        return when {
            single != null -> BodyIdentification.Matched(single.institutionName, single.result)
            matches.isEmpty() -> BodyIdentification.NoMatch
            else -> BodyIdentification.Ambiguous
        }
    }

    /**
     * Runs one parser and turns any exception it throws into "not
     * recognized". Cancellation is rethrown so a cancelled import or check
     * still stops, and errors that are not exceptions (out of memory, for
     * example) are left alone. The caught exception is deliberately dropped
     * without logging: see the class comment.
     */
    private fun parseGuarded(parser: SmsParser, sender: String, body: String, receivedAt: Long): ParseResult? =
        try {
            parser.parse(sender, body, receivedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
