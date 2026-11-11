package com.sajib.smsexpensetracker.parser

import com.sajib.smsexpensetracker.parser.core.ParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic fuzz test: every success body in [ParserTestMessages] is
 * damaged at random, with a fixed seed, a few thousand times in total. No
 * parser, called directly and so without the engine's guard, and neither
 * engine method may ever throw, and the whole run must stay fast. Whether a
 * damaged body is still recognized is not judged here.
 */
class ParserFuzzTest {

    private val parsers = ParserTestMessages.allParsers()
    private val engine = ParserEngine(parsers)

    @Test
    fun `no parser and neither engine method throws on any mutated body, and the run stays fast`() {
        val mutator = BodyMutator(SEED)
        val failures = mutableListOf<String>()
        var mutatedBodies = 0
        var changedBodies = 0
        var recognized = 0
        var unrecognized = 0

        val startedAt = System.nanoTime()
        ParserTestMessages.transactions.forEach { sample ->
            repeat(MUTATIONS_PER_BODY) {
                val mutated = mutator.mutate(sample.body)
                mutatedBodies++
                if (mutated.text != sample.body) changedBodies++

                fun guard(what: String, call: () -> Any?): Any? = try {
                    call()
                } catch (e: Throwable) {
                    failures += "$what threw ${e.javaClass.simpleName} after ${mutated.mutations} on: ${mutated.text}"
                    null
                }

                parsers.forEach { parser ->
                    guard("parser ${parser.institutionName}") {
                        parser.parse(parser.senderPatterns.first(), mutated.text, receivedAt = 0L)
                    }
                }
                val routed = guard("engine.parse") { engine.parse(sample.sender, mutated.text, receivedAt = 0L) }
                guard("engine.identifyByBody") { engine.identifyByBody(mutated.text, receivedAt = 0L) }
                if (routed != null) recognized++ else unrecognized++
            }
        }
        val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI

        assertTrue(
            "${failures.size} failures, first ${FAILURES_SHOWN}:\n" + failures.take(FAILURES_SHOWN).joinToString("\n"),
            failures.isEmpty()
        )
        assertEquals(ParserTestMessages.transactions.size * MUTATIONS_PER_BODY, mutatedBodies)
        assertTrue("only $changedBodies of $mutatedBodies bodies were changed", changedBodies > mutatedBodies * 9 / 10)
        assertTrue("recognized $recognized, unrecognized $unrecognized", recognized > 0 && unrecognized > 0)
        assertTrue("took $elapsedMillis ms", elapsedMillis < MAX_RUN_MILLIS)
    }

    @Test
    fun `the same seed gives the same mutations`() {
        val body = ParserTestMessages.transactions.first().body
        val first = BodyMutator(SEED)
        val second = BodyMutator(SEED)

        repeat(MUTATIONS_PER_BODY) {
            assertEquals(first.mutate(body), second.mutate(body))
        }
    }

    private companion object {
        const val SEED = 20261111L
        const val MUTATIONS_PER_BODY = 100
        const val MAX_RUN_MILLIS = 3_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val FAILURES_SHOWN = 10
    }
}
