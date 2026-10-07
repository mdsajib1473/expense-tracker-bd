package com.sajib.smsexpensetracker.capture

import androidx.compose.runtime.Composable
import javax.inject.Inject

/**
 * Play flavor live capture: does nothing. The play build declares no SMS
 * permission (AGENT.md rule 11), so the content is shown directly and no
 * capture ever starts.
 */
class PlayLiveCaptureSetup @Inject constructor() : LiveCaptureSetup {

    @Composable
    override fun PermissionGate(onReady: () -> Unit, content: @Composable () -> Unit) {
        content()
    }

    override suspend fun startCapture() = Unit
}
