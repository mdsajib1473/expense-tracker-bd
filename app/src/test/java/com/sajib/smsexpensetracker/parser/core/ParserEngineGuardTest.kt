package com.sajib.smsexpensetracker.parser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

/**
 * Covers the engine's defensive boundary: a parser that throws counts as
 * not recognizing the message, in the sender-routed path and in the
 * body-only path, and the exception text never reaches the caller. Every
 * value is invented.
 */
class ParserEngineGuardTest {

    /** Throws whatever [failure] builds from the body, as a buggy parser might. */
    private class ThrowingParser(
        override val institutionName: String,
        override val senderPatterns: List<String>,
        private val failure: (body: String) -> Throwable
    ) : SmsParser {
        var calls = 0

        override fun parse(sender: String, body: String, receivedAt: Long): ParseResult? {
            calls++
            throw failure(body)
        }
    }

    /** Recognizes every body. */
    private class AcceptingParser(
        override val institutionName: String,
        override val senderPatterns: List<String>
    ) : SmsParser {
        override fun parse(sender: String, body: String, receivedAt: Long): ParseResult = ParseResult(
            type = TransactionType.CREDIT,
            amount = BigDecimal("10.00"),
            balance = BigDecimal("20.00"),
            counterparty = null,
            reference = "REF0000",
            rawSms = body
        )
    }

    /** A Throwable that is not an Exception, like the JVM's own fatal errors. */
    private class FatalTestError : Error()

    private val secretBody = "Your verification code is 482615"

    private fun throwing(failure: (String) -> Throwable) =
        ThrowingParser("Sample Bank", listOf("SAMPLEBANK"), failure)

    @Test
    fun `sender routed parse treats a throwing parser as not recognizing the message`() {
        val failures: List<(String) -> Throwable> = listOf(
            { body -> NumberFormatException(body) },
            { body -> IllegalStateException(body) },
            { body -> IndexOutOfBoundsException(body) },
            { body -> ArithmeticException(body) },
            { body -> RuntimeException(body) }
        )

        failures.forEach { failure ->
            val engine = ParserEngine(listOf(throwing(failure)))

            assertNull(engine.parse("SAMPLEBANK", secretBody, receivedAt = 0L))
        }
    }

    @Test
    fun `sender routed parse does not fall through to another parser when the routed one throws`() {
        val routed = throwing { body -> IllegalStateException(body) }
        val other = AcceptingParser("Sample Wallet", listOf("SAMPLEWALLET"))

        val result = ParserEngine(listOf(routed, other)).parse("SAMPLEBANK", secretBody, receivedAt = 0L)

        assertNull(result)
        assertEquals(1, routed.calls)
    }

    @Test
    fun `sender routed parse still returns the result of a parser that does not throw`() {
        val engine = ParserEngine(
            listOf(throwing { body -> IllegalStateException(body) }, AcceptingParser("Sample Wallet", listOf("SAMPLEWALLET")))
        )

        assertEquals("Sample Wallet", engine.parse("SAMPLEWALLET", secretBody, receivedAt = 0L)?.institutionName)
    }

    @Test
    fun `body only identification skips a throwing parser and continues with the others`() {
        val failing = throwing { body -> IllegalStateException(body) }
        val engine = ParserEngine(listOf(failing, AcceptingParser("Sample Wallet", listOf("SAMPLEWALLET"))))

        val identification = engine.identifyByBody(secretBody, receivedAt = 0L)

        assertEquals("Sample Wallet", (identification as BodyIdentification.Matched).institutionName)
        assertEquals(1, failing.calls)
    }

    @Test
    fun `body only identification is no match when every parser throws`() {
        val engine = ParserEngine(
            listOf(
                throwing { body -> IllegalStateException(body) },
                ThrowingParser("Sample Wallet", listOf("SAMPLEWALLET")) { body -> IndexOutOfBoundsException(body) }
            )
        )

        assertEquals(BodyIdentification.NoMatch, engine.identifyByBody(secretBody, receivedAt = 0L))
    }

    @Test
    fun `body only identification is still ambiguous when two parsers accept and a third throws`() {
        val engine = ParserEngine(
            listOf(
                AcceptingParser("Sample Wallet A", listOf("WALLET-A")),
                throwing { body -> IllegalStateException(body) },
                AcceptingParser("Sample Wallet B", listOf("WALLET-B"))
            )
        )

        assertEquals(BodyIdentification.Ambiguous, engine.identifyByBody(secretBody, receivedAt = 0L))
    }

    @Test
    fun `cancellation thrown inside a parser propagates from both paths`() {
        val cancellation = CancellationException("cancelled")
        val engine = ParserEngine(listOf(throwing { cancellation }))

        try {
            engine.parse("SAMPLEBANK", secretBody, receivedAt = 0L)
            fail("expected the cancellation to propagate from parse")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
        try {
            engine.identifyByBody(secretBody, receivedAt = 0L)
            fail("expected the cancellation to propagate from identifyByBody")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun `an error that is not an exception is not swallowed`() {
        val error = FatalTestError()
        val engine = ParserEngine(listOf(throwing { error }))

        try {
            engine.parse("SAMPLEBANK", secretBody, receivedAt = 0L)
            fail("expected the error to propagate from parse")
        } catch (e: FatalTestError) {
            assertSame(error, e)
        }
        try {
            engine.identifyByBody(secretBody, receivedAt = 0L)
            fail("expected the error to propagate from identifyByBody")
        } catch (e: FatalTestError) {
            assertSame(error, e)
        }
    }

    @Test
    fun `nothing the engine returns carries the exception text`() {
        val engine = ParserEngine(listOf(throwing { body -> IllegalStateException("failed on: $body") }))

        val routed = engine.parse("SAMPLEBANK", secretBody, receivedAt = 0L)
        val bodyOnly = engine.identifyByBody(secretBody, receivedAt = 0L)

        assertNull(routed)
        assertFalse(bodyOnly.toString().contains("482615"))
        assertTrue(bodyOnly is BodyIdentification.NoMatch)
    }
}
