package com.sajib.smsexpensetracker.data.backup

import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.smsRaw
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.SmsBackupMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.ByteArrayInputStream

/**
 * Covers reading SMS Backup & Restore XML with kxml2, the parser Android's
 * own is derived from, on the plain JVM. Every sender, number, OTP and amount
 * is invented.
 */
class SmsBackupReaderTest {

    private val reader = SmsBackupReader { KXmlParser() }

    private fun entries(xml: String): List<SmsBackupEntry> = reader.open(xml.byteInputStream()).entries.toList()

    private fun inboxBody(xml: String): String {
        val entry = entries(xml).single()
        assertTrue(entry is SmsBackupEntry.Inbox)
        return (entry as SmsBackupEntry.Inbox).message.body
    }

    private fun assertFailsWith(error: BackupImportError, secrets: List<String>, block: () -> Unit) {
        val e = assertThrows(BackupImportException::class.java) { block() }
        assertEquals(error, e.error)
        assertNull(e.cause)
        for (secret in secrets) {
            assertFalse(e.message.orEmpty().contains(secret))
            assertFalse(e.toString().contains(secret))
        }
    }

    @Test
    fun `prolog is skipped and the declared count is only a hint`() {
        val xml = SmsBackupXml.PROLOG + SmsBackupXml.rootStart(count = 5) +
            sms(body = "first") + "\n<!-- a comment between messages -->\n" + sms(body = "second") +
            SmsBackupXml.ROOT_END

        val document = reader.open(xml.byteInputStream())

        assertEquals(5, document.countHint)
        assertEquals(2, document.entries.toList().size)
    }

    @Test
    fun `missing or unusable count gives no hint`() {
        assertNull(reader.open(document(sms(body = "x"), count = null).byteInputStream()).countHint)
        val badCount = SmsBackupXml.PROLOG + "<smses count=\"many\">" + SmsBackupXml.ROOT_END
        assertNull(reader.open(badCount.byteInputStream()).countHint)
    }

    @Test
    fun `byte order mark before the declaration is ignored`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            document(sms(body = "hello")).toByteArray()

        val entries = reader.open(ByteArrayInputStream(bytes)).entries.toList()

        assertEquals(1, entries.size)
    }

    @Test
    fun `received sms becomes an inbox entry with sender, body and date`() {
        val entry = entries(document(sms(address = "bKash", body = "Sample body", date = 1_733_047_200_000L))).single()

        assertEquals(SmsBackupEntry.Inbox(SmsBackupMessage("bKash", "Sample body", 1_733_047_200_000L)), entry)
    }

    @Test
    fun `sent, draft and other types are skipped, as is an mms element`() {
        val mms = "<mms date=\"1700000000000\" msg_box=\"1\" address=\"01700000001\">" +
            "<parts><part seq=\"0\" ct=\"text/plain\" text=\"Sample\" /></parts></mms>"
        val xml = document(
            sms(type = "2", body = "sent"),
            sms(type = "3", body = "draft"),
            sms(type = "4", body = "outbox"),
            sms(type = "5", body = "failed"),
            sms(type = "6", body = "queued"),
            sms(type = null, body = "no type"),
            mms,
            sms(body = "received")
        )

        val entries = entries(xml)

        assertEquals(List(7) { SmsBackupEntry.NotInbox }, entries.take(7))
        assertTrue(entries[7] is SmsBackupEntry.Inbox)
    }

    @Test
    fun `received sms missing sender, body or a valid date is invalid`() {
        val xml = document(
            smsRaw(address = null, body = "b", date = "1700000000000", type = "1"),
            smsRaw(address = "  ", body = "b", date = "1700000000000", type = "1"),
            smsRaw(address = "bKash", body = null, date = "1700000000000", type = "1"),
            smsRaw(address = "bKash", body = "b", date = null, type = "1"),
            smsRaw(address = "bKash", body = "b", date = "yesterday", type = "1"),
            smsRaw(address = "bKash", body = "b", date = "0", type = "1"),
            smsRaw(address = "bKash", body = "b", date = "-5", type = "1")
        )

        assertEquals(List(7) { SmsBackupEntry.Invalid }, entries(xml))
    }

    @Test
    fun `newline references produce a multi-line body`() {
        val body = inboxBody(document(smsRaw("NAGAD", "Cash In Received.&#10;Amount: Tk 5000.00", "1700000000000", "1")))

        assertEquals("Cash In Received.\nAmount: Tk 5000.00", body)
    }

    @Test
    fun `apostrophe, quote and other entities are decoded`() {
        val body = inboxBody(document(smsRaw("NAGAD", "Payment to &apos;Sample&apos; &quot;Shop&quot; &amp; &lt;Co&gt;", "1700000000000", "1")))

        assertEquals("Payment to 'Sample' \"Shop\" & <Co>", body)
    }

    /**
     * kxml2 2.3.0 truncates any reference above U+FFFF to one char, so the
     * decoded emoji itself is checked with the platform parser in
     * SmsBackupReaderPlatformParserTest; here only the parse must survive.
     */
    @Test
    fun `emoji written as two surrogate references does not fail the parse`() {
        val decimal = inboxBody(document(smsRaw("bKash", "Hi &#55357;&#56832;!", "1700000000000", "1")))
        val hex = inboxBody(document(smsRaw("bKash", "Hi &#xD83D;&#xDE00;!", "1700000000000", "1")))

        assertTrue(decimal.startsWith("Hi ") && decimal.endsWith("!"))
        assertTrue(hex.startsWith("Hi ") && hex.endsWith("!"))
    }

    @Test
    fun `lone surrogate references decode to the replacement character`() {
        val body = inboxBody(document(smsRaw("bKash", "a&#55357;b&#56832;c", "1700000000000", "1")))

        assertEquals("a\uFFFDb\uFFFDc", body)
    }

    @Test
    fun `Bangla text is decoded as UTF-8`() {
        val bangla = "সতর্কতা: আপনার পিন বা কোড কাউকে দেবেন না।"

        assertEquals(bangla, inboxBody(document(sms(address = "NAGAD", body = bangla))))
    }

    @Test
    fun `invalid UTF-8 bytes are replaced instead of failing the file`() {
        val xml = document(sms(body = "before#after"))
        val bytes = xml.toByteArray().map { if (it == '#'.code.toByte()) 0xFF.toByte() else it }.toByteArray()

        val entry = reader.open(ByteArrayInputStream(bytes)).entries.single() as SmsBackupEntry.Inbox

        assertEquals("before\uFFFDafter", entry.message.body)
    }

    @Test
    fun `wrong root element is not a backup and the error holds no content`() {
        val xml = SmsBackupXml.PROLOG + "<messages>" + sms(body = "Your code is 246810") + "</messages>"

        assertFailsWith(BackupImportError.NOT_SMS_BACKUP, listOf("246810", "Your code")) { reader.open(xml.byteInputStream()) }
    }

    @Test
    fun `empty input, plain text and a doctype are not a backup`() {
        assertFailsWith(BackupImportError.NOT_SMS_BACKUP, emptyList()) { reader.open("".byteInputStream()) }
        assertFailsWith(BackupImportError.NOT_SMS_BACKUP, emptyList()) { reader.open("just some text".byteInputStream()) }
        val doctype = "<?xml version='1.0'?><!DOCTYPE smses [<!ENTITY a \"aaaa\">]><smses>&a;</smses>"
        assertFailsWith(BackupImportError.NOT_SMS_BACKUP, emptyList()) { reader.open(doctype.byteInputStream()) }
    }

    @Test
    fun `file cut inside a message is malformed and the error holds no content`() {
        val full = document(sms(body = "Your bKash verification code is 246810. Expires in 2 minutes."))
        val cut = full.substring(0, full.indexOf("246810") + 3)

        assertFailsWith(BackupImportError.MALFORMED, listOf("246", "verification")) {
            reader.open(cut.byteInputStream()).entries.toList()
        }
    }

    @Test
    fun `file cut after a complete message yields that message, then fails as malformed`() {
        val full = document(sms(body = "first"), sms(body = "second"))
        val cut = full.substring(0, full.indexOf("<sms", startIndex = full.indexOf("first")))
        val iterator = reader.open(cut.byteInputStream()).entries.iterator()

        assertTrue(iterator.next() is SmsBackupEntry.Inbox)
        val e = assertThrows(BackupImportException::class.java) { iterator.next() }
        assertEquals(BackupImportError.MALFORMED, e.error)
    }

    @Test
    fun `malformed references and unknown entities are typed errors without content`() {
        val bodies = listOf("code 246810 &#;", "code 246810 &#12a;", "code 246810 &secret246810;", "code 246810 & more")
        for (body in bodies) {
            val xml = document(smsRaw("bKash", body, "1700000000000", "1"))
            assertFailsWith(BackupImportError.MALFORMED, listOf("246810", "code")) {
                reader.open(xml.byteInputStream()).entries.toList()
            }
        }
    }

    @Test
    fun `entries are parsed lazily while the stream is read`() {
        val stream = SmsBackupXml.GeneratedBackup(count = 50_000) { i ->
            sms(body = "Synthetic message number $i with some padding text to make it longer", date = 1_700_000_000_000L + i)
        }

        val first = reader.open(stream).entries.first()

        assertTrue(first is SmsBackupEntry.Inbox)
        assertTrue("read ${stream.bytesRead} bytes for one message", stream.bytesRead < 64 * 1024)
    }

    @Test
    fun `message toString shows neither body nor sender`() {
        val text = SmsBackupMessage("01700000009", "Your code is 246810", 1L).toString()

        assertFalse(text.contains("246810"))
        assertFalse(text.contains("01700000009"))
    }
}
