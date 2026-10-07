package com.sajib.smsexpensetracker.capture

import androidx.compose.runtime.Composable

/**
 * Flavor-specific live SMS capture setup. The full flavor requests the SMS
 * permission (rationale dialog first, Hard Constraint 2) and starts capture;
 * the play flavor declares no SMS permission, so its implementation does
 * nothing. Shared code depends only on this interface, never on SmsReceiver.
 */
interface LiveCaptureSetup {

    /**
     * Shows [content] once capture may start, calling [onReady] each time the
     * prerequisites (for example the SMS permission) are satisfied.
     */
    @Composable
    fun PermissionGate(onReady: () -> Unit, content: @Composable () -> Unit)

    /** Starts capture work after [PermissionGate] reported ready. */
    suspend fun startCapture()
}
