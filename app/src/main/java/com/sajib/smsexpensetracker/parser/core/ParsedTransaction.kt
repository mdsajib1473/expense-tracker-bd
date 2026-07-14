package com.sajib.smsexpensetracker.parser.core

/**
 * Pairs a ParseResult with the institution name of the parser that produced
 * it, since ParseResult itself carries no reference back to its source.
 */
data class ParsedTransaction(
    val institutionName: String,
    val result: ParseResult
)
