package com.sajib.smsexpensetracker.ui.onboarding

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Covers showing onboarding once on first launch and again on request, with real SharedPreferences. */
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    private val tracker = FirstLaunchTracker(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun `onboarding shows on first launch`() {
        assertTrue(OnboardingViewModel(tracker).visible.value)
    }

    @Test
    fun `closing hides it and it does not show on the next launch`() {
        OnboardingViewModel(tracker).close()

        assertTrue(tracker.hasCompletedOnboarding())
        assertFalse(OnboardingViewModel(tracker).visible.value)
    }

    @Test
    fun `it can be shown again from the menu and closed again`() {
        OnboardingViewModel(tracker).close()
        val vm = OnboardingViewModel(tracker)

        vm.show()
        assertTrue(vm.visible.value)
        vm.close()

        assertFalse(vm.visible.value)
        assertFalse(OnboardingViewModel(tracker).visible.value)
    }

    @Test
    fun `onboarding and the inbox import are tracked separately`() {
        OnboardingViewModel(tracker).close()

        assertFalse(tracker.hasImportedHistory())
    }
}
