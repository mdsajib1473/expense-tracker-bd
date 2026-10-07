package com.sajib.smsexpensetracker.data.backup

import java.io.IOException
import java.io.Reader
import java.util.Locale

/**
 * Sits between the decoded backup text and the XML parser and rewrites the
 * numeric character references that XML 1.0 forbids, so one emoji or stray
 * control code cannot fail a whole import.
 *
 * SMS Backup & Restore writes an emoji as two references, one per UTF-16
 * surrogate (for example "&#55357;&#56832;"), which a strict parser rejects.
 *
 * - A high surrogate reference immediately followed by a low surrogate
 *   reference becomes one reference to the combined code point, in the radix
 *   of the high one ("&#55357;&#56832;" becomes "&#128512;",
 *   "&#xD83D;&#xDE00;" becomes "&#x1F600;").
 * - A lone surrogate reference, or a reference to any other code point XML 1.0
 *   does not allow (controls below 0x20 except tab, LF and CR, 0xFFFE, 0xFFFF,
 *   anything above 0x10FFFF), becomes [REPLACEMENT].
 * - Everything else passes through unchanged: every valid reference, every
 *   named entity, and every incomplete or malformed reference, which is left
 *   for the parser to reject.
 *
 * Reads through a fixed size buffer with at most two references of lookahead,
 * so memory stays constant and a reference split across reads is handled.
 */
internal class SanitizingXmlReader(
    private val source: Reader,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) : Reader() {

    private val buffer = CharArray(bufferSize)
    private var bufferPos = 0
    private var bufferEnd = 0
    private var sourceExhausted = false
    private var pushedBack = NONE

    /** Rewritten or passed-through reference text waiting to be returned. */
    private val pending = StringBuilder()
    private var pendingPos = 0

    /** Exact source text of the reference being scanned. */
    private val raw = StringBuilder()

    init {
        require(bufferSize > 0) { "bufferSize must be positive" }
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > cbuf.size - off) throw IndexOutOfBoundsException()
        if (len == 0) return 0

        var written = 0
        while (written < len) {
            if (pendingPos < pending.length) {
                val count = minOf(len - written, pending.length - pendingPos)
                pending.toCharArray(cbuf, off + written, pendingPos, pendingPos + count)
                pendingPos += count
                written += count
                if (pendingPos == pending.length) {
                    pending.setLength(0)
                    pendingPos = 0
                }
                continue
            }

            if (pushedBack == NONE && bufferPos < bufferEnd) {
                val limit = minOf(bufferEnd, bufferPos + (len - written))
                var end = bufferPos
                while (end < limit && buffer[end] != '&') end++
                if (end > bufferPos) {
                    val count = end - bufferPos
                    System.arraycopy(buffer, bufferPos, cbuf, off + written, count)
                    bufferPos = end
                    written += count
                    continue
                }
            }

            // Return what is ready instead of blocking on the source for more.
            if (written > 0 && pushedBack == NONE && bufferPos >= bufferEnd) break

            val c = nextChar()
            if (c == EOF) break
            if (c == '&'.code) {
                handleAmpersand()
            } else {
                cbuf[off + written] = c.toChar()
                written++
            }
        }
        return if (written == 0) EOF else written
    }

    override fun close() {
        source.close()
    }

    /** Called after an '&' was consumed; leaves the output text in [pending]. */
    private fun handleAmpersand() {
        var scan = scanReference()
        while (true) {
            if (scan == null || isXmlChar(scan.codePoint)) {
                pending.append(raw)
                return
            }
            if (scan.codePoint !in HIGH_SURROGATES) {
                pending.append(REPLACEMENT)
                return
            }

            val high = scan
            if (peekChar() != '&'.code) {
                pending.append(REPLACEMENT)
                return
            }
            nextChar()
            scan = scanReference()
            if (scan != null && scan.codePoint in LOW_SURROGATES) {
                appendReference(Character.toCodePoint(high.codePoint.toChar(), scan.codePoint.toChar()), high.hex)
                return
            }
            // The high surrogate is lone; the reference after it is handled on its own.
            pending.append(REPLACEMENT)
        }
    }

    /**
     * Scans "#digits;" or "#xhexdigits;" after an '&' into [raw] (which then
     * holds the exact source text, '&' included). Returns null if the text is
     * not a complete numeric reference; the char that ended the scan is then
     * pushed back so it is processed normally.
     */
    private fun scanReference(): Scan? {
        raw.setLength(0)
        raw.append('&')

        var c = nextChar()
        if (c != '#'.code) return incomplete(c)
        raw.append('#')

        c = nextChar()
        val hex = c == 'x'.code
        if (hex) {
            raw.append('x')
            c = nextChar()
        }

        var value = 0
        var digits = 0
        while (digits < MAX_DIGITS) {
            val digit = digitValue(c, hex)
            if (digit < 0) break
            raw.append(c.toChar())
            value = minOf(value * (if (hex) 16 else 10) + digit, OUT_OF_RANGE)
            digits++
            c = nextChar()
        }

        if (digits == 0 || c != ';'.code) return incomplete(c)
        raw.append(';')
        return Scan(value, hex)
    }

    private fun incomplete(c: Int): Scan? {
        if (c != EOF) pushedBack = c
        return null
    }

    private fun appendReference(codePoint: Int, hex: Boolean) {
        if (hex) {
            pending.append("&#x").append(Integer.toHexString(codePoint).uppercase(Locale.ROOT)).append(';')
        } else {
            pending.append("&#").append(codePoint).append(';')
        }
    }

    private fun nextChar(): Int {
        if (pushedBack != NONE) {
            val c = pushedBack
            pushedBack = NONE
            return c
        }
        if (bufferPos >= bufferEnd && !fill()) return EOF
        return buffer[bufferPos++].code
    }

    private fun peekChar(): Int {
        val c = nextChar()
        if (c != EOF) pushedBack = c
        return c
    }

    private fun fill(): Boolean {
        if (sourceExhausted) return false
        var count: Int
        do {
            count = source.read(buffer, 0, buffer.size)
        } while (count == 0)
        if (count < 0) {
            sourceExhausted = true
            return false
        }
        bufferPos = 0
        bufferEnd = count
        return true
    }

    /** A complete numeric reference: its value, clamped to [OUT_OF_RANGE], and radix. */
    private class Scan(val codePoint: Int, val hex: Boolean)

    internal companion object {
        /** What every illegal reference is rewritten to: U+FFFD REPLACEMENT CHARACTER. */
        const val REPLACEMENT = "&#xFFFD;"

        private const val DEFAULT_BUFFER_SIZE = 8192
        private const val EOF = -1
        private const val NONE = -2

        /**
         * Longest digit run treated as a reference. Longer runs are passed
         * through untouched for the parser to judge; no legal code point needs
         * more than 7 digits without leading zeros.
         */
        private const val MAX_DIGITS = 16
        private const val OUT_OF_RANGE = 0x110000

        private val HIGH_SURROGATES = 0xD800..0xDBFF
        private val LOW_SURROGATES = 0xDC00..0xDFFF

        /** The XML 1.0 Char production. */
        private fun isXmlChar(codePoint: Int): Boolean =
            codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD ||
                codePoint in 0x20..0xD7FF ||
                codePoint in 0xE000..0xFFFD ||
                codePoint in 0x10000..0x10FFFF

        /** ASCII digits only: Character.digit would also accept Bangla and other digits. */
        private fun digitValue(c: Int, hex: Boolean): Int = when (c) {
            in '0'.code..'9'.code -> c - '0'.code
            in 'a'.code..'f'.code -> if (hex) c - 'a'.code + 10 else -1
            in 'A'.code..'F'.code -> if (hex) c - 'A'.code + 10 else -1
            else -> -1
        }
    }
}
