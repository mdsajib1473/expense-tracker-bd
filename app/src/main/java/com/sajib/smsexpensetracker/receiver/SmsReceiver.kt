package com.sajib.smsexpensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Delegates every incoming SMS to ParserEngine only, per AGENT.md
 * architecture. Never calls an institution parser directly.
 *
 * Logs only whether a match happened and, if so, the institution name, per
 * Hard Constraint 1. The raw SMS body and sender address never reach
 * Logcat, in debug or release builds.
 *
 * Telephony.Sms.Intents.getMessagesFromIntent does NOT reassemble multi-part
 * long SMS on its own, it returns one SmsMessage per part, all belonging to
 * one logical message in the same intent. This receiver concatenates every
 * part's body before handing it to ParserEngine, so a long bKash SMS that
 * splits mid-message is parsed as a whole, not dropped.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var parserEngine: ParserEngine
    @Inject lateinit var repository: TransactionRepository

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
                val parsed = parserEngine.parse(sender, body, receivedAt)
                if (parsed != null) {
                    repository.save(parsed, receivedAt)
                    Log.d(TAG, "Matched: ${parsed.institutionName}")
                } else {
                    Log.d(TAG, "No match")
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