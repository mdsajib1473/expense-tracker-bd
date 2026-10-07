package com.sajib.smsexpensetracker.data.backup

import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.SmsBackupMessage
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.CodingErrorAction
import javax.inject.Inject

/**
 * Streams the messages of an XML export made by the SMS Backup & Restore app.
 *
 * The file is a root `<smses>` element (its optional count attribute is only
 * a hint) holding one flat `<sms>` element per message, whose address, body,
 * date (epoch millis) and type (1 = received) attributes are read; all other
 * attributes are ignored. An XML declaration, processing instructions and
 * comments may precede the root.
 *
 * The bytes are decoded as UTF-8 here, with malformed input replaced rather
 * than rejected, so the declared encoding is irrelevant, and then pass
 * through [SanitizingXmlReader] so surrogate and control character
 * references cannot fail the parse. Nothing is buffered beyond one message.
 *
 * Every failure surfaces as a [BackupImportException] carrying a category and
 * position only: parser exception messages embed attribute values, which are
 * SMS bodies, so they are never passed on.
 */
class SmsBackupReader @Inject constructor(
    private val parserProvider: XmlParserProvider
) {

    /**
     * Reads the prolog and root element of [input] and returns the document,
     * whose entries are then parsed one at a time as they are iterated. The
     * entries can be iterated once. [input] is not closed.
     *
     * @throws BackupImportException when the input is not a backup file, here
     * or while iterating when it turns out damaged or truncated, or when
     * reading fails.
     */
    fun open(input: InputStream): SmsBackupDocument {
        val parser = parserProvider.newParser()
        guarded(parser) {
            parser.setInput(decodedText(input))
            skipProlog(parser)
        }
        if (parser.name != ROOT) throw notBackup(parser)
        val countHint = attribute(parser, ATTR_COUNT)?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
        return SmsBackupDocument(countHint, entries(parser))
    }

    private fun entries(parser: XmlPullParser): Sequence<SmsBackupEntry> = sequence {
        while (true) {
            when (guarded(parser) { parser.next() }) {
                XmlPullParser.START_TAG -> {
                    val entry = if (parser.name == SMS) readSms(parser) else SmsBackupEntry.NotInbox
                    guarded(parser) { skipRestOfElement(parser) }
                    yield(entry)
                }
                // Children are consumed whole, so the only end tag seen here is the root's.
                XmlPullParser.END_TAG -> return@sequence
                XmlPullParser.END_DOCUMENT -> throw truncated(parser)
                else -> Unit
            }
        }
    }.constrainOnce()

    private fun readSms(parser: XmlPullParser): SmsBackupEntry {
        if (attribute(parser, ATTR_TYPE) != TYPE_INBOX) return SmsBackupEntry.NotInbox

        val address = attribute(parser, ATTR_ADDRESS)
        val body = attribute(parser, ATTR_BODY)
        val receivedAt = attribute(parser, ATTR_DATE)?.trim()?.toLongOrNull()?.takeIf { it > 0 }
        if (address.isNullOrBlank() || body == null || receivedAt == null) return SmsBackupEntry.Invalid

        return SmsBackupEntry.Inbox(SmsBackupMessage(address, body, receivedAt))
    }

    /** Skips comments, processing instructions and whitespace up to the root start tag. */
    private fun skipProlog(parser: XmlPullParser) {
        while (true) {
            when (parser.nextToken()) {
                XmlPullParser.START_TAG -> return
                XmlPullParser.COMMENT,
                XmlPullParser.PROCESSING_INSTRUCTION,
                XmlPullParser.IGNORABLE_WHITESPACE -> Unit
                XmlPullParser.TEXT -> if (!parser.isWhitespace) throw notBackup(parser)
                // A DOCTYPE is rejected too: backups never have one, and its
                // entity declarations are an expansion attack surface.
                else -> throw notBackup(parser)
            }
        }
    }

    /** Consumes everything up to and including the end tag of the element just started. */
    private fun skipRestOfElement(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> throw truncated(parser)
            }
        }
    }

    /** Looks attributes up by local name, so it works with or without namespace processing. */
    private fun attribute(parser: XmlPullParser, name: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i) == name) return parser.getAttributeValue(i)
        }
        return null
    }

    private fun decodedText(input: InputStream): Reader {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        val text = BufferedReader(InputStreamReader(input, decoder))
        // Java keeps a UTF-8 byte order mark as a character, which the parser
        // would reject in front of the XML declaration.
        text.mark(1)
        if (text.read() != BYTE_ORDER_MARK) text.reset()
        return SanitizingXmlReader(text)
    }

    /**
     * Runs parser calls, translating every way a parser reports bad input
     * into a content-free [BackupImportException]. kxml2 signals some
     * malformed character references with unchecked exceptions
     * (NumberFormatException, StringIndexOutOfBoundsException), hence the
     * RuntimeException branch; only parser calls run inside [block].
     */
    private inline fun <T> guarded(parser: XmlPullParser, block: () -> T): T =
        try {
            block()
        } catch (e: XmlPullParserException) {
            throw BackupImportException(BackupImportError.MALFORMED, e.lineNumber.orNull(), e.columnNumber.orNull())
        } catch (e: IOException) {
            throw BackupImportException(BackupImportError.READ_FAILED)
        } catch (e: RuntimeException) {
            throw BackupImportException(BackupImportError.MALFORMED, parser.lineNumber.orNull(), parser.columnNumber.orNull())
        }

    private fun notBackup(parser: XmlPullParser) =
        BackupImportException(BackupImportError.NOT_SMS_BACKUP, parser.lineNumber.orNull(), parser.columnNumber.orNull())

    private fun truncated(parser: XmlPullParser) =
        BackupImportException(BackupImportError.MALFORMED, parser.lineNumber.orNull(), parser.columnNumber.orNull())

    /** Parsers report an unknown position as -1. */
    private fun Int.orNull(): Int? = takeIf { it > 0 }

    private companion object {
        const val ROOT = "smses"
        const val SMS = "sms"
        const val ATTR_COUNT = "count"
        const val ATTR_TYPE = "type"
        const val ATTR_ADDRESS = "address"
        const val ATTR_BODY = "body"
        const val ATTR_DATE = "date"
        const val TYPE_INBOX = "1"
        const val BYTE_ORDER_MARK = 0xFEFF
    }
}
