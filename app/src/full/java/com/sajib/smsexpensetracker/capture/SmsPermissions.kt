package com.sajib.smsexpensetracker.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Full flavor only. The SMS permissions live capture needs, and what Android reports about them. */
object SmsPermissions {

    /** RECEIVE_SMS for new messages and READ_SMS for the one-time inbox import. */
    val REQUIRED: Array<String> = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

    /** True only when every permission in [REQUIRED] is granted. */
    fun areGranted(context: Context): Boolean = REQUIRED.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * True when Android says a rationale should be shown for any permission
     * in [REQUIRED], which it does after the user has denied it once and
     * while it will still show its prompt.
     */
    fun shouldShowRationale(activity: Activity): Boolean = REQUIRED.any { permission ->
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
