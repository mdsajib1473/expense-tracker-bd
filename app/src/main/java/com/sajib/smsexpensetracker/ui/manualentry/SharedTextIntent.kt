package com.sajib.smsexpensetracker.ui.manualentry

import android.content.Intent
import android.util.Log

/**
 * Reads the text of a message shared to this app (ACTION_SEND, text/plain).
 *
 * The intent comes from another app and is untrusted. Only
 * [Intent.EXTRA_TEXT] is read, as a plain string; every other extra, the
 * clip data and the data URI are ignored, so no file or content URI is ever
 * opened. The text itself is never logged.
 */
internal object SharedTextIntent {

    private const val TAG = "SharedText"
    private const val TEXT_PLAIN = "text/plain"

    /**
     * The shared text, or null when [intent] is not a text/plain
     * ACTION_SEND at all. A share with no EXTRA_TEXT, or with extras that
     * cannot be read, gives an empty string, so the check reports that
     * there was no text instead of the share being silently ignored.
     */
    fun extractText(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND) return null
        if (Intent.normalizeMimeType(intent.type) != TEXT_PLAIN) return null
        return try {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        } catch (e: RuntimeException) {
            // Reading one extra unparcels the sender's whole extras bundle on
            // older Android versions, which fails on a class this app does not
            // have. The exception text is not logged because it may describe
            // the sender's extras.
            Log.w(TAG, "Shared extras could not be read")
            ""
        }
    }

    /**
     * True when [intent] should be handled: it is not the original launch
     * intent replayed after recreation or when the task is reopened from
     * Recents, both of which would add the same message a second time.
     */
    fun isFreshLaunch(intent: Intent, isRecreated: Boolean): Boolean =
        !isRecreated && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
}
