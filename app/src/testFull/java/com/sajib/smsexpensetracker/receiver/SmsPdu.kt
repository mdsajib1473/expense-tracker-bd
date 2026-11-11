package com.sajib.smsexpensetracker.receiver

import android.content.Intent
import android.provider.Telephony
import java.io.ByteArrayOutputStream

/**
 * Builds 3GPP SMS-DELIVER PDUs for receiver tests, so an SMS_RECEIVED
 * broadcast reaches the receiver in the form the telephony stack delivers
 * it and is decoded by the real platform code. Senders may be alphanumeric
 * (letters, digits and spaces, as banks use) or short numeric codes. A text
 * body is sent as UCS-2 and split into parts like a multi-part SMS, with no
 * concatenation header, because the receiver joins the parts of one
 * broadcast in order.
 */
internal object SmsPdu {

    private const val EXTRA_PDUS = "pdus"
    private const val EXTRA_FORMAT = "format"
    private const val FORMAT_3GPP = "3gpp"

    private const val NO_SERVICE_CENTRE = 0x00
    private const val SMS_DELIVER_NO_MORE_MESSAGES = 0x04
    private const val TYPE_ALPHANUMERIC = 0xD0
    private const val TYPE_UNKNOWN_NUMBER = 0x81
    private const val PROTOCOL_DEFAULT = 0x00
    private const val CODING_UCS2 = 0x08

    /** Compressed user data (coding bit 5), which the platform does not decode, so the message has no text body. */
    private const val CODING_COMPRESSED = 0x20
    private const val BCD_FILLER = 0x0F
    private const val SEPTET_BITS = 7
    private const val OCTET_BITS = 8
    private const val SEMI_OCTET_BITS = 4
    private const val UCS2_CHARS_PER_PART = 70

    /** 11 November 2026, 12:00:00, UTC: each field in swapped BCD, then the zone. */
    private val TIMESTAMP = byteArrayOf(0x62, 0x11, 0x11, 0x21, 0x00, 0x00, 0x00)

    /** The parts of a text message carrying [body] from [sender], as many as it needs. */
    fun textParts(sender: String, body: String): List<ByteArray> =
        body.chunked(UCS2_CHARS_PER_PART).ifEmpty { listOf("") }.map { part ->
            deliver(sender, CODING_UCS2, part.toByteArray(Charsets.UTF_16BE))
        }

    /** One message from [sender] whose user data is not text, for which the platform reports no text body. */
    fun nonTextPart(sender: String, data: ByteArray): ByteArray = deliver(sender, CODING_COMPRESSED, data)

    /** An SMS_RECEIVED broadcast carrying [parts] as one message. */
    fun smsReceivedIntent(parts: List<ByteArray>): Intent =
        Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            .putExtra(EXTRA_PDUS, parts.toTypedArray())
            .putExtra(EXTRA_FORMAT, FORMAT_3GPP)

    /** An SMS_RECEIVED broadcast carrying the text [body] from [sender]. */
    fun smsReceivedIntent(sender: String, body: String): Intent = smsReceivedIntent(textParts(sender, body))

    private fun deliver(sender: String, coding: Int, userData: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(NO_SERVICE_CENTRE)
        out.write(SMS_DELIVER_NO_MORE_MESSAGES)
        out.write(address(sender))
        out.write(PROTOCOL_DEFAULT)
        out.write(coding)
        out.write(TIMESTAMP)
        out.write(userData.size)
        out.write(userData)
        return out.toByteArray()
    }

    /** Address length in semi-octets, type of address, then the packed address value. */
    private fun address(sender: String): ByteArray =
        if (sender.all(Char::isDigit)) {
            byteArrayOf(sender.length.toByte(), TYPE_UNKNOWN_NUMBER.toByte()) + swappedBcd(sender)
        } else {
            val usefulSemiOctets = (sender.length * SEPTET_BITS + SEMI_OCTET_BITS - 1) / SEMI_OCTET_BITS
            byteArrayOf(usefulSemiOctets.toByte(), TYPE_ALPHANUMERIC.toByte()) + packedSeptets(sender)
        }

    /** Digits two per octet, the first in the low half, padded with the filler nibble. */
    private fun swappedBcd(digits: String): ByteArray =
        digits.chunked(2).map { pair ->
            val low = pair[0].digitToInt()
            val high = if (pair.length == 2) pair[1].digitToInt() else BCD_FILLER
            ((high shl SEMI_OCTET_BITS) or low).toByte()
        }.toByteArray()

    /**
     * GSM 7-bit packing, least significant bits first. Letters, digits and
     * the space have the same code in the GSM default alphabet as in ASCII,
     * which covers every sender used by the tests.
     */
    private fun packedSeptets(text: String): ByteArray {
        val packed = ByteArray((text.length * SEPTET_BITS + OCTET_BITS - 1) / OCTET_BITS)
        text.forEachIndexed { index, char ->
            val septet = char.code and 0x7F
            val bitOffset = index * SEPTET_BITS
            val octet = bitOffset / OCTET_BITS
            val shift = bitOffset % OCTET_BITS
            packed[octet] = (packed[octet].toInt() or (septet shl shift)).toByte()
            if (shift > OCTET_BITS - SEPTET_BITS) {
                packed[octet + 1] = (packed[octet + 1].toInt() or (septet shr (OCTET_BITS - shift))).toByte()
            }
        }
        return packed
    }
}
