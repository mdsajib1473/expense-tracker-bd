package com.sajib.smsexpensetracker.capture

import androidx.compose.runtime.Composable

/**
 * Flavor-specific live SMS capture setup. The full flavor offers an optional
 * onboarding step that requests the SMS permission (rationale dialog first,
 * Hard Constraint 2) and, once it is granted, runs the one-time inbox
 * import; the play flavor declares no SMS permission, so its implementation
 * does nothing. Live capture never gates the rest of the app: the
 * transaction list, backup import, share and paste work with or without
 * it. Shared code depends only on this interface, never on SmsReceiver.
 */
interface LiveCaptureSetup {

    /** True when this build offers the optional onboarding step [OnboardingStep]. */
    val hasOnboardingStep: Boolean

    /** The optional onboarding step that turns on live capture. Shows nothing when [hasOnboardingStep] is false. */
    @Composable
    fun OnboardingStep()

    /**
     * Starts capture work if its prerequisites (for example the SMS
     * permission) are met, and otherwise does nothing. Safe to call on
     * every resume; one-time work runs once.
     */
    suspend fun startCapture()
}
