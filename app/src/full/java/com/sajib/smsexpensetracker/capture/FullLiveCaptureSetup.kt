package com.sajib.smsexpensetracker.capture

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import com.sajib.smsexpensetracker.importer.InboxImporter
import com.sajib.smsexpensetracker.ui.common.SmsAccessOnboardingStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Full flavor live capture. The SMS permission is optional: it is requested
 * only from the onboarding step (rationale dialog first), and declining or
 * never answering leaves the rest of the app fully usable. Once both SMS
 * permissions are granted, [startCapture] runs the one-time inbox import.
 * Live messages are captured by SmsReceiver, registered in the full
 * manifest, which the system only calls while RECEIVE_SMS is granted.
 */
class FullLiveCaptureSetup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inboxImporter: InboxImporter,
    private val firstLaunchTracker: FirstLaunchTracker
) : LiveCaptureSetup {

    private val startMutex = Mutex()

    override val hasOnboardingStep: Boolean = true

    @Composable
    override fun OnboardingStep() {
        SmsAccessOnboardingStep()
    }

    /**
     * Runs the one-time inbox import if both SMS permissions are granted and
     * it has not finished before. Calls never overlap, so a quick second
     * resume cannot start a second import.
     */
    override suspend fun startCapture() {
        startMutex.withLock {
            if (!SmsPermissions.areGranted(context)) return
            if (firstLaunchTracker.hasImportedHistory()) return
            Log.i(TAG, "Inbox import started")
            inboxImporter.importInbox()
            firstLaunchTracker.markHistoryImported()
            Log.i(TAG, "Inbox import finished")
        }
    }

    private companion object {
        const val TAG = "LiveCapture"
    }
}
