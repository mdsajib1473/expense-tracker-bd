package com.sajib.smsexpensetracker.parser

import java.util.Random

/**
 * Seeded random text damage for parser robustness tests. The same seed gives
 * the same sequence of mutations on every machine and every run, because
 * java.util.Random's algorithm is fixed by its specification.
 */
internal class BodyMutator(seed: Long) {

    /** One kind of damage. */
    enum class Mutation {
        /** Removes one character. */
        DELETE_CHARACTER,

        /** Inserts a comma, a dot or a space. */
        INSERT_SEPARATOR,

        /** Replaces every digit of one digit run, or of one whole number, with a comma or a dot. */
        DIGITS_TO_SEPARATORS,

        /** Cuts the text short. */
        TRUNCATE,

        /** Repeats a short piece of the text right after itself. */
        DUPLICATE_SEGMENT
    }

    /** A damaged text and the mutations that produced it, in order. */
    data class Mutated(val text: String, val mutations: List<Mutation>)

    private val random = Random(seed)

    /** Applies one to [MAX_MUTATIONS] random mutations to [body], each on top of the previous one. */
    fun mutate(body: String): Mutated {
        val mutations = List(1 + random.nextInt(MAX_MUTATIONS)) { Mutation.entries[random.nextInt(Mutation.entries.size)] }
        return Mutated(mutations.fold(body, ::damage), mutations)
    }

    private fun damage(text: String, mutation: Mutation): String = when (mutation) {
        Mutation.DELETE_CHARACTER ->
            if (text.isEmpty()) text else text.removeRange(random.nextInt(text.length).let { it..it })
        Mutation.INSERT_SEPARATOR -> {
            val at = random.nextInt(text.length + 1)
            text.substring(0, at) + SEPARATORS[random.nextInt(SEPARATORS.length)] + text.substring(at)
        }
        Mutation.DIGITS_TO_SEPARATORS -> {
            val targets = (if (random.nextBoolean()) DIGIT_RUN else NUMBER).findAll(text).toList()
            if (targets.isEmpty()) {
                text
            } else {
                val target = targets[random.nextInt(targets.size)]
                val separator = NUMBER_SEPARATORS[random.nextInt(NUMBER_SEPARATORS.length)].toString()
                text.replaceRange(target.range, target.value.replace(DIGIT, separator))
            }
        }
        Mutation.TRUNCATE -> text.substring(0, random.nextInt(text.length + 1))
        Mutation.DUPLICATE_SEGMENT ->
            if (text.isEmpty()) {
                text
            } else {
                val start = random.nextInt(text.length)
                val end = minOf(text.length, start + 1 + random.nextInt(MAX_SEGMENT_LENGTH))
                text.substring(0, end) + text.substring(start, end) + text.substring(end)
            }
    }

    private companion object {
        const val MAX_MUTATIONS = 3
        const val MAX_SEGMENT_LENGTH = 12
        const val SEPARATORS = ",. "
        const val NUMBER_SEPARATORS = ",."
        val DIGIT = Regex("""\d""")
        val DIGIT_RUN = Regex("""\d+""")
        val NUMBER = Regex("""\d+(?:[,.]\d+)*""")
    }
}
