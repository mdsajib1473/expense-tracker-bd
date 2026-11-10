package com.sajib.smsexpensetracker.parser.core

/**
 * Outcome of identifying a message from its body alone, with no sender
 * address, as produced by [ParserEngine.identifyByBody].
 */
sealed interface BodyIdentification {

    /**
     * Exactly one registered parser recognized the body.
     *
     * [toString] leaves out [result] on purpose: it carries the raw message,
     * which must never reach a log or a crash report (AGENT.md Hard
     * Constraint 1).
     */
    data class Matched(val institutionName: String, val result: ParseResult) : BodyIdentification {
        override fun toString(): String = "Matched(institutionName=$institutionName)"
    }

    /** No registered parser recognized the body. */
    data object NoMatch : BodyIdentification

    /** More than one parser recognized the body, so its provider cannot be told safely. */
    data object Ambiguous : BodyIdentification
}
