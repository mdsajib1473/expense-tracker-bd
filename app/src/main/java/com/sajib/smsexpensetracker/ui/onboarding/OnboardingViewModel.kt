package com.sajib.smsexpensetracker.ui.onboarding

import androidx.lifecycle.ViewModel
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Decides when the onboarding screens show: once on first launch until the
 * user finishes or skips them, and again whenever the user opens "How to
 * add transactions" from the menu.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val firstLaunchTracker: FirstLaunchTracker
) : ViewModel() {

    private val _visible = MutableStateFlow(!firstLaunchTracker.hasCompletedOnboarding())

    /** True while the onboarding screens are shown instead of the transaction list. */
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** Shows the onboarding screens again, for example from the menu. */
    fun show() {
        _visible.value = true
    }

    /** Closes the onboarding screens, finished or skipped, and never shows them on launch again. */
    fun close() {
        firstLaunchTracker.markOnboardingCompleted()
        _visible.value = false
    }
}
