package com.sajib.smsexpensetracker.capture

import androidx.compose.runtime.Composable
import javax.inject.Inject

/**
 * Play flavor live capture: does nothing. The play build declares no SMS
 * permission (AGENT.md rule 11), so there is no onboarding step and no
 * capture ever starts.
 */
class PlayLiveCaptureSetup @Inject constructor() : LiveCaptureSetup {

    override val hasOnboardingStep: Boolean = false

    @Composable
    override fun OnboardingStep() = Unit

    override suspend fun startCapture() = Unit
}
