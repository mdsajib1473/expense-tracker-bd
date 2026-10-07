package com.sajib.smsexpensetracker.data.backup

import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.document
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.sms
import com.sajib.smsexpensetracker.data.backup.SmsBackupXml.smsRaw
import com.sajib.smsexpensetracker.di.BackupImportModule
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

/**
 * Runs the reader with the parser production uses (the android.util.Xml
 * binding from BackupImportModule) under Robolectric, for the behavior
 * where it differs from kxml2 2.3.0: references above U+FFFF, and the
 * exceptions it throws. Every value is invented.
 */
@RunWith(RobolectricTestRunner::class)
class SmsBackupReaderPlatformParserTest {

    private val reader = SmsBackupReader(BackupImportModule.provideXmlParserProvider())
    private val emoji = String(Character.toChars(0x1F600))
    private val replacement = String(Character.toChars(0xFFFD))

    private fun inboxBody(xml: String): String =
        (reader.open(xml.byteInputStream()).entries.single() as SmsBackupEntry.Inbox).message.body

    private fun assertMalformedWithoutContent(xml: String, secret: String) {
        val e = assertThrows(BackupImportException::class.java) { reader.open(xml.byteInputStream()).entries.toList() }
        assertEquals(BackupImportError.MALFORMED, e.error)
        assertFalse(e.toString().contains(secret))
    }

    @Test
    fun `emoji written as two surrogate references decodes to the emoji`() {
        assertEquals("Hi $emoji!", inboxBody(document(smsRaw("bKash", "Hi &#55357;&#56832;!", "1700000000000", "1"))))
        assertEquals("Hi $emoji!", inboxBody(document(smsRaw("bKash", "Hi &#xD83D;&#xDE00;!", "1700000000000", "1"))))
    }

    @Test
    fun `a single reference to the emoji also decodes to the emoji`() {
        assertEquals("Hi $emoji!", inboxBody(document(smsRaw("bKash", "Hi &#x1F600;!", "1700000000000", "1"))))
    }

    @Test
    fun `lone surrogate references decode to the replacement character`() {
        val body = inboxBody(document(smsRaw("bKash", "a&#55357;b&#56832;c", "1700000000000", "1")))

        assertEquals("a${replacement}b${replacement}c", body)
    }

    @Test
    fun `byte order mark, prolog, newlines and entities work with the platform parser`() {
        val xml = document(smsRaw("NAGAD", "Payment to &apos;Sample Ministry&apos; is Successful.&#10;Amount: Tk  107.07", "1700000000000", "1"))
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + xml.toByteArray()

        val entry = reader.open(ByteArrayInputStream(bytes)).entries.single() as SmsBackupEntry.Inbox

        assertEquals("Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07", entry.message.body)
    }

    @Test
    fun `file cut after a complete message is malformed even though the parser reports a normal end`() {
        val full = document(sms(body = "first"), sms(body = "second"))
        val cut = full.substring(0, full.indexOf("<sms", startIndex = full.indexOf("first")))
        val iterator = reader.open(cut.byteInputStream()).entries.iterator()

        assertTrue(iterator.next() is SmsBackupEntry.Inbox)
        val e = assertThrows(BackupImportException::class.java) { iterator.next() }
        assertEquals(BackupImportError.MALFORMED, e.error)
    }

    @Test
    fun `damaged files are typed errors whose message holds no content`() {
        val full = document(sms(body = "Your bKash verification code is 246810. Expires in 2 minutes."))
        assertMalformedWithoutContent(full.substring(0, full.indexOf("246810") + 3), "246")
        for (body in listOf("code 246810 &#;", "code 246810 &#12a;", "code 246810 &secret246810;", "code 246810 & more")) {
            assertMalformedWithoutContent(document(smsRaw("bKash", body, "1700000000000", "1")), "246810")
        }
    }

    @Test
    fun `doctype and wrong root are not a backup`() {
        val doctype = "<?xml version='1.0'?><!DOCTYPE smses [<!ENTITY a \"aaaa\">]><smses>&a;</smses>"
        val wrongRoot = SmsBackupXml.PROLOG + "<calls></calls>"

        for (xml in listOf(doctype, wrongRoot)) {
            val e = assertThrows(BackupImportException::class.java) { reader.open(xml.byteInputStream()) }
            assertEquals(BackupImportError.NOT_SMS_BACKUP, e.error)
        }
    }
}
