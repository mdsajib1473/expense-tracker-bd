package com.sajib.smsexpensetracker.importer

import android.content.Context
import android.provider.Telephony
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads the existing SMS inbox once, routing every message through the same
 * ParserEngine and TransactionRepository that SmsReceiver uses for live
 * messages, so parsing/saving logic exists in exactly one place.
 *
 * Requires READ_SMS to already be granted; the caller is responsible for
 * checking permission and first-launch state before calling run().
 */
class SmsImportJob @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parserEngine: ParserEngine,
    private val repository: TransactionRepository
) {

    suspend fun run() = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null,
            null,
            null
        )

        cursor?.use {
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(addressIndex) ?: continue
                val body = it.getString(bodyIndex) ?: continue
                val receivedAt = it.getLong(dateIndex)

                val parsed = parserEngine.parse(sender, body, receivedAt)
                if (parsed != null) {
                    repository.save(parsed, receivedAt)
                }
            }
        }
    }
}
