package com.sajib.smsexpensetracker.capture

import android.content.Context
import androidx.core.content.edit

/**
 * Full flavor only. Remembers that the SMS permission has been requested at
 * least once on this install, which [SmsAccessPolicy] needs to tell "never
 * asked" from "Android no longer asks". One flag in SharedPreferences; it
 * holds no message data.
 */
class SmsPermissionRequestStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once a request for the SMS permission has returned a result. */
    fun wasRequested(): Boolean = prefs.getBoolean(KEY_REQUESTED, false)

    /** Records that a request for the SMS permission has returned a result. */
    fun markRequested() {
        prefs.edit { putBoolean(KEY_REQUESTED, true) }
    }

    private companion object {
        const val PREFS_NAME = "sms_access_prefs"
        const val KEY_REQUESTED = "sms_permission_requested"
    }
}
