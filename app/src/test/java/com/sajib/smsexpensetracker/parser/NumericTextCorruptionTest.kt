package com.sajib.smsexpensetracker.parser

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive, deterministic checks that damaged numbers and cut-off messages
 * never make a parser throw. Every body in [ParserTestMessages] is damaged
 * in every possible place and handed to every parser, not only its own,
 * because the body-only path runs all of them. Only "does not throw" is
 * asserted here: whether a damaged body is still recognized is each
 * parser's own rule. Every value in the bodies is invented.
 */
class NumericTextCorruptionTest {

    private val parsers = ParserTestMessages.allParsers()
    private val bodies = (ParserTestMessages.transactions + ParserTestMessages.nonTransactions).map { it.body }

    /** Runs every parser on [body] and returns one line per parser that threw. */
    private fun parsersThatThrowOn(body: String): List<String> = parsers.mapNotNull { parser ->
        try {
            parser.parse(parser.senderPatterns.first(), body, receivedAt = 0L)
            null
        } catch (e: Throwable) {
            "${parser.institutionName} threw ${e.javaClass.simpleName} on: $body"
        }
    }

    private fun assertNoFailures(failures: List<String>) {
        assertTrue(
            "${failures.size} failures, first ${FAILURES_SHOWN}:\n" + failures.take(FAILURES_SHOWN).joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun `replacing any number with separators never makes a parser throw`() {
        val failures = bodies.flatMap { body ->
            NUMBER.findAll(body).toList().flatMap { number ->
                REPLACEMENTS.flatMap { replace ->
                    parsersThatThrowOn(body.replaceRange(number.range, replace(number.value)))
                }
            }
        }

        assertNoFailures(failures)
    }

    @Test
    fun `no message cut short at any length makes a parser throw`() {
        val failures = bodies.flatMap { body ->
            (0..body.length).flatMap { length -> parsersThatThrowOn(body.substring(0, length)) }
        }

        assertNoFailures(failures)
    }

    @Test
    fun `no message with its beginning missing makes a parser throw`() {
        val failures = bodies.flatMap { body ->
            (0..body.length).flatMap { start -> parsersThatThrowOn(body.substring(start)) }
        }

        assertNoFailures(failures)
    }

    private companion object {
        const val FAILURES_SHOWN = 10

        /** A number as banks print it: digit groups joined by single commas or dots, such as "4,495.00". */
        val NUMBER = Regex("""\d+(?:[,.]\d+)*""")

        val DIGIT = Regex("""\d""")

        /** Ways to damage one number: separators only, in place of the digits or of the whole number. */
        val REPLACEMENTS: List<(String) -> String> = listOf(
            { _ -> "," },
            { _ -> "." },
            { _ -> ",." },
            { _ -> "" },
            { number -> ",".repeat(number.length) },
            { number -> number.replace(DIGIT, ",") },
            { number -> number.replace(DIGIT, ".") }
        )
    }
}
