package com.sajib.smsexpensetracker.capture

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test

/** Play flavor only. Live capture does not exist here: no onboarding step and nothing starts. */
class PlayLiveCaptureSetupTest {

    private val setup = PlayLiveCaptureSetup()

    @Test
    fun `play flavor offers no live capture onboarding step`() {
        assertFalse(setup.hasOnboardingStep)
    }

    @Test
    fun `starting capture does nothing and does not fail`() = runTest {
        setup.startCapture()
    }
}
