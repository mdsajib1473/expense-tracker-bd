package com.sajib.smsexpensetracker.parser.core

import org.junit.Assert.assertNull
import org.junit.Test

/**
 * At M0 there are no registered parsers yet. This test only proves the
 * engine compiles and behaves correctly with an empty parser list, it is not
 * testing any real institution's parsing logic, that starts at M1.
 */
class ParserEngineTest {

    @Test
    fun `parse returns null when no parsers are registered`() {
        val engine = ParserEngine(parsers = emptyList())

        val result = engine.parse(
            sender = "bKash",
            body = "You have received Tk 500.00 from 017XXXXXXXX",
            receivedAt = System.currentTimeMillis()
        )

        assertNull(result)
    }
}
