package com.sajib.smsexpensetracker.importer

import android.content.Context
import android.provider.Telephony
import com.sajib.smsexpensetracker.domain.usecase.IngestSmsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Full flavor only. Reads the existing SMS inbox once, routing every message
 * through the same ingestion entry point (IngestSmsUseCase) that SmsReceiver
 * uses for live messages, so parsing, de-duplication and saving exist in
 * exactly one place.
 *
 * Requires READ_SMS to already be granted; the caller is responsible for
 * checking permission and first-launch state before calling run().
 */
class SmsImportJob @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ingestSms: IngestSmsUseCase
) {

    /** Ingests every inbox SMS on the IO dispatcher. */
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

                ingestSms(sender, body, receivedAt)
            }
        }
    }
}
