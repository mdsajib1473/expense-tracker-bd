package com.sajib.smsexpensetracker.importer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Tracks whether the one-time SMS history import has already run, so it
 * fires once on first launch, not every time the app opens. Not transaction
 * data, so SharedPreferences is used instead of Room.
 */
class FirstLaunchTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasImportedHistory(): Boolean = prefs.getBoolean(KEY_IMPORTED, false)

    fun markHistoryImported() {
        prefs.edit().putBoolean(KEY_IMPORTED, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_IMPORTED = "history_imported"
    }
}
