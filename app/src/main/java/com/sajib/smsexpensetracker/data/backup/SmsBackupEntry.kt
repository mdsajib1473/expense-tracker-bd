package com.sajib.smsexpensetracker.data.backup

import com.sajib.smsexpensetracker.domain.model.SmsBackupMessage

/** One child element of the backup root, classified by [SmsBackupReader]. */
sealed interface SmsBackupEntry {

    /** A received SMS (type="1") with a sender, a body and a valid date. */
    data class Inbox(val message: SmsBackupMessage) : SmsBackupEntry

    /** A sent, draft or other non-inbox SMS, or a non-SMS element such as an MMS. Its content is not read. */
    data object NotInbox : SmsBackupEntry

    /** A received SMS missing its sender, its body or a valid date. */
    data object Invalid : SmsBackupEntry
}
