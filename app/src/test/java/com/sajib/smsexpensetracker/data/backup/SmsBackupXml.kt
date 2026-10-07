package com.sajib.smsexpensetracker.data.backup

import java.io.InputStream

/**
 * Builds synthetic SMS Backup & Restore XML for tests, escaped the way that
 * app writes it: XML special characters as entities, newlines as "&#10;"
 * and every character outside the BMP (an emoji, say) as two surrogate
 * references. Every sender, number and amount used with it is invented.
 */
internal object SmsBackupXml {

    /** Declaration, comments and stylesheet instruction, as the app writes them. */
    const val PROLOG = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n" +
        "<!--File Created By SMS Backup & Restore v0.0.0 on 01/01/2024 00:00:00-->\n" +
        "<!--To view this file in a more readable format, open it with a web browser-->\n" +
        "<?xml-stylesheet type=\"text/xsl\" href=\"sms.xsl\"?>\n"

    const val DEFAULT_DATE = 1_700_000_000_000L

    /** A whole backup file holding [elements], declaring [count] in the root. */
    fun document(vararg elements: String, count: Int? = elements.size): String =
        PROLOG + rootStart(count) + elements.joinToString(separator = "") { "  $it\n" } + ROOT_END

    /** An `<sms>` element whose [body] is escaped here. */
    fun sms(
        address: String? = "bKash",
        body: String? = "",
        date: Long? = DEFAULT_DATE,
        type: String? = "1"
    ): String = smsRaw(address?.let(::escape), body?.let(::escape), date?.toString(), type)

    /**
     * An `<sms>` element with every attribute value written exactly as given,
     * so a test can put raw character references into it. A null value
     * leaves the attribute out. Includes the attributes the reader ignores.
     */
    fun smsRaw(address: String?, body: String?, date: String?, type: String?): String = buildString {
        append("<sms protocol=\"0\"")
        if (address != null) append(" address=\"").append(address).append('"')
        if (date != null) append(" date=\"").append(date).append('"')
        if (type != null) append(" type=\"").append(type).append('"')
        append(" subject=\"null\"")
        if (body != null) append(" body=\"").append(body).append('"')
        append(" toa=\"null\" sc_toa=\"null\" service_center=\"null\" read=\"1\" status=\"-1\" locked=\"0\"")
        append(" date_sent=\"0\" sub_id=\"1\" readable_date=\"1 Jan 2024 00:00:00\" contact_name=\"(Unknown)\" />")
    }

    /** Escapes [text] for an attribute value the way SMS Backup & Restore does. */
    fun escape(text: String): String = buildString {
        for (c in text) {
            when {
                c == '&' -> append("&amp;")
                c == '<' -> append("&lt;")
                c == '>' -> append("&gt;")
                c == '"' -> append("&quot;")
                c == '\'' -> append("&apos;")
                c == '\n' -> append("&#10;")
                c == '\r' -> append("&#13;")
                c.isSurrogate() -> append("&#").append(c.code).append(';')
                else -> append(c)
            }
        }
    }

    fun rootStart(count: Int?): String =
        if (count == null) "<smses>\n" else "<smses count=\"$count\">\n"

    const val ROOT_END = "</smses>\n"

    /**
     * Streams a backup of [count] messages built by [element] without ever
     * holding the whole file, and records how many bytes were handed out.
     */
    class GeneratedBackup(
        private val count: Int,
        private val element: (Int) -> String
    ) : InputStream() {

        /** Bytes handed out to the reader so far. */
        var bytesRead = 0L
            private set

        /** Messages generated so far, whether or not the reader has parsed them yet. */
        val messagesGenerated: Int
            get() = next

        private var next = 0
        private var chunk = (PROLOG + rootStart(count)).toByteArray()
        private var chunkPos = 0
        private var finished = false

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (chunkPos >= chunk.size) {
                if (finished) return -1
                chunk = if (next < count) {
                    "  ${element(next++)}\n".toByteArray()
                } else {
                    finished = true
                    ROOT_END.toByteArray()
                }
                chunkPos = 0
            }
            val n = minOf(len, chunk.size - chunkPos)
            System.arraycopy(chunk, chunkPos, b, off, n)
            chunkPos += n
            bytesRead += n
            return n
        }
    }
}
