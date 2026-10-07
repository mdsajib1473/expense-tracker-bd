package com.sajib.smsexpensetracker.capture

import androidx.compose.runtime.Composable
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import com.sajib.smsexpensetracker.importer.SmsImportJob
import com.sajib.smsexpensetracker.ui.common.SmsPermissionGate
import javax.inject.Inject

/**
 * Full flavor live capture: gates the UI behind the SMS permission flow
 * (rationale dialog first) and, once granted, runs the one-time inbox import.
 * Live messages are captured by SmsReceiver, registered in the full manifest.
 */
class FullLiveCaptureSetup @Inject constructor(
    private val importJob: SmsImportJob,
    private val firstLaunchTracker: FirstLaunchTracker
) : LiveCaptureSetup {

    @Composable
    override fun PermissionGate(onReady: () -> Unit, content: @Composable () -> Unit) {
        SmsPermissionGate(onPermissionGranted = onReady, content = content)
    }

    override suspend fun startCapture() {
        if (!firstLaunchTracker.hasImportedHistory()) {
            importJob.run()
            firstLaunchTracker.markHistoryImported()
        }
    }
}
