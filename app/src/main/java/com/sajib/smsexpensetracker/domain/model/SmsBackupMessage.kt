package com.sajib.smsexpensetracker.domain.model

/**
 * One received SMS read from a backup file, ready for the ingestion entry
 * point. [receivedAtMillis] is the SMS timestamp from the backup (epoch
 * millis), not the time of the import.
 *
 * Not a data class on purpose: [toString] must never print the body, which
 * may hold an OTP or a PIN (AGENT.md rule 12), nor the sender address.
 */
class SmsBackupMessage(
    val sender: String,
    val body: String,
    val receivedAtMillis: Long
) {

    override fun equals(other: Any?): Boolean =
        other is SmsBackupMessage &&
            sender == other.sender &&
            body == other.body &&
            receivedAtMillis == other.receivedAtMillis

    override fun hashCode(): Int = (sender.hashCode() * 31 + body.hashCode()) * 31 + receivedAtMillis.hashCode()

    override fun toString(): String = "SmsBackupMessage(receivedAtMillis=$receivedAtMillis)"
}
