package com.sajib.smsexpensetracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.Reader
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Covers the character reference rewriting in front of the XML parser. Plain
 * JVM, no Android runtime needed.
 */
class SanitizingXmlReaderTest {

    private val replacement = SanitizingXmlReader.REPLACEMENT

    private fun sanitize(input: String, bufferSize: Int = 8192): String =
        SanitizingXmlReader(StringReader(input), bufferSize).use { it.readText() }

    /** Pulls one char per call out of a reader whose source also yields one char per call. */
    private fun sanitizeOneCharAtATime(input: String, bufferSize: Int): String {
        val reader = SanitizingXmlReader(OneCharPerReadReader(input), bufferSize)
        val out = StringBuilder()
        val one = CharArray(1)
        while (true) {
            val count = reader.read(one, 0, 1)
            if (count < 0) break
            out.append(one, 0, count)
        }
        return out.toString()
    }

    private class OneCharPerReadReader(text: String) : Reader() {
        private val source = StringReader(text)
        override fun read(cbuf: CharArray, off: Int, len: Int): Int =
            if (len == 0) 0 else source.read(cbuf, off, 1)

        override fun close() = source.close()
    }

    @Test
    fun `surrogate pair in decimal form becomes one reference to the combined code point`() {
        assertEquals("a&#128512;b", sanitize("a&#55357;&#56832;b"))
    }

    @Test
    fun `surrogate pair in hex form becomes one hex reference to the combined code point`() {
        assertEquals("a&#x1F600;b", sanitize("a&#xD83D;&#xDE00;b"))
        assertEquals("&#x1F600;", sanitize("&#xd83d;&#xde00;"))
    }

    @Test
    fun `lone high surrogate becomes the replacement character`() {
        assertEquals("x${replacement}y", sanitize("x&#55357;y"))
        assertEquals("x$replacement", sanitize("x&#xD83D;"))
    }

    @Test
    fun `lone low surrogate becomes the replacement character`() {
        assertEquals("x${replacement}y", sanitize("x&#56832;y"))
        assertEquals("$replacement$replacement", sanitize("&#56832;&#55357;"))
    }

    @Test
    fun `high surrogate followed by a non surrogate reference keeps that reference`() {
        assertEquals("$replacement&#10;", sanitize("&#55357;&#10;"))
        assertEquals("$replacement&amp;", sanitize("&#55357;&amp;"))
    }

    @Test
    fun `second of two high surrogates still pairs with the low surrogate after it`() {
        assertEquals("$replacement&#128512;", sanitize("&#55357;&#55357;&#56832;"))
    }

    @Test
    fun `references to code points XML forbids become the replacement character`() {
        val forbidden = listOf("&#0;", "&#1;", "&#x1F;", "&#8;", "&#xFFFE;", "&#xFFFF;", "&#x110000;", "&#99999999;")
        for (reference in forbidden) {
            assertEquals(reference, replacement, sanitize(reference))
        }
    }

    @Test
    fun `valid references and named entities pass through unchanged`() {
        val valid = "&#10;&#x1F600;&#128512;&amp;&apos;&quot;&lt;&gt;&#9;&#13;&#xa;&#0010;&#55295;&#xE000;&#xFFFD;&#x10FFFF;"
        assertEquals(valid, sanitize(valid))
    }

    @Test
    fun `incomplete or malformed references pass through unchanged for the parser to judge`() {
        val malformed = "&#;&#x;&#12&#X41;&# 1;& &#x1G;&#55357&#56832;&"
        assertEquals("&#;&#x;&#12&#X41;&# 1;& &#x1G;&#55357$replacement&", sanitize(malformed))
    }

    @Test
    fun `references split across buffer boundaries are rewritten the same way`() {
        val input = "ab&#55357;&#56832;cd&#10;&#xD83D;&#xDE00;e&#56832;f&amp;"
        val expected = "ab&#128512;cd&#10;&#x1F600;e${replacement}f&amp;"
        for (bufferSize in 1..input.length + 1) {
            assertEquals("buffer $bufferSize", expected, sanitize(input, bufferSize))
            assertEquals("one char at a time, buffer $bufferSize", expected, sanitizeOneCharAtATime(input, bufferSize))
        }
    }

    @Test
    fun `plain text including Bangla passes through unchanged`() {
        val text = "Plain text, সতর্কতা: আপনার পিন কাউকে দেবেন না। <tag attr=\"v\"/> 100% & more"
        assertEquals(text, sanitize(text))
    }

    @Test
    fun `empty input yields empty output`() {
        assertEquals("", sanitize(""))
        assertEquals(-1, SanitizingXmlReader(StringReader("")).read(CharArray(4), 0, 4))
    }

    @Test
    fun `strict parser rejects the raw surrogate references but accepts the sanitized text`() {
        val xml = "<r a=\"&#55357;&#56832; &#xDE00;\"/>"

        assertThrows(SAXParseException::class.java) { strictlyParseAttribute(StringReader(xml)) }
        val value = strictlyParseAttribute(SanitizingXmlReader(StringReader(xml)))

        assertEquals("\uD83D\uDE00 \uFFFD", value)
    }

    /** Parses with the JDK's validating-grade XML 1.0 parser and returns attribute "a" of the root. */
    private fun strictlyParseAttribute(reader: Reader): String {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        builder.setErrorHandler(object : ErrorHandler {
            override fun warning(exception: SAXParseException) = Unit
            override fun error(exception: SAXParseException) = throw exception
            override fun fatalError(exception: SAXParseException) = throw exception
        })
        return builder.parse(InputSource(reader)).documentElement.getAttribute("a")
    }
}
