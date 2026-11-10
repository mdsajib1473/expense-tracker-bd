package com.sajib.smsexpensetracker.importer

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Tracks one-time first-launch events, so each fires once, not every time
 * the app opens: the SMS history import (full flavor) and the onboarding
 * screens (both flavors). Not transaction data, so SharedPreferences is
 * used instead of Room.
 */
class FirstLaunchTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the one-time SMS inbox import has finished. */
    fun hasImportedHistory(): Boolean = prefs.getBoolean(KEY_IMPORTED, false)

    /** Records that the one-time SMS inbox import has finished. */
    fun markHistoryImported() {
        prefs.edit { putBoolean(KEY_IMPORTED, true) }
    }

    /** True once the user has finished or skipped the onboarding screens. */
    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    /** Records that the user has finished or skipped the onboarding screens. */
    fun markOnboardingCompleted() {
        prefs.edit { putBoolean(KEY_ONBOARDING_DONE, true) }
    }

    private companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_IMPORTED = "history_imported"
        const val KEY_ONBOARDING_DONE = "onboarding_completed"
    }
}
