package com.sajib.smsexpensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.sajib.smsexpensetracker.domain.model.IngestResult
import com.sajib.smsexpensetracker.domain.usecase.IngestSmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full flavor only. Hands every incoming SMS to the single ingestion entry
 * point (IngestSmsUseCase), which runs ParserEngine and writes through the
 * repository. Never calls an institution parser directly.
 *
 * Logs only the outcome and, for a recognized SMS, the institution name, per
 * Hard Constraint 1. The raw SMS body and sender address never reach
 * Logcat, in debug or release builds.
 *
 * Telephony.Sms.Intents.getMessagesFromIntent does NOT reassemble multi-part
 * long SMS on its own, it returns one SmsMessage per part, all belonging to
 * one logical message in the same intent. This receiver concatenates every
 * part's body before ingestion, so a long bKash SMS that splits mid-message
 * is parsed as a whole, not dropped.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var ingestSms: IngestSmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody }
        val receivedAt = messages[0].timestampMillis

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (val result = ingestSms(sender, body, receivedAt)) {
                    is IngestResult.Saved -> Log.d(TAG, "Matched: ${result.institutionName}")
                    is IngestResult.Duplicate -> Log.d(TAG, "Duplicate: ${result.institutionName}")
                    IngestResult.Unrecognized -> Log.d(TAG, "No match")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
