package com.sajib.smsexpensetracker.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Full flavor only. What the optional onboarding step can offer for SMS access. */
enum class SmsAccessState {

    /** Both SMS permissions are granted. */
    GRANTED,

    /** Not granted, and Android will still show its permission prompt when asked. */
    CAN_REQUEST,

    /** Not granted, and Android no longer shows its prompt, so only app settings can grant it. */
    PERMANENTLY_DENIED
}

/**
 * Full flavor only. Decides what to offer for SMS access. Kept out of the
 * Compose code so it can be unit tested: [stateFor] only combines three
 * facts, and [currentState] reads them.
 */
object SmsAccessPolicy {

    private const val PACKAGE_SCHEME = "package"

    /**
     * Android has no direct "permanently denied" answer, so it is inferred:
     * the permission is not [granted], the app has [requestedBefore], and
     * Android still says no rationale should be shown ([shouldShowRationale]
     * is false), which is what it reports once it stops showing its prompt.
     * Before the first request the same answer only means "never asked", so
     * nothing is inferred then.
     *
     * Known limit: closing the system prompt without choosing is not
     * recorded by Android as a denial, so afterwards it looks the same and
     * is reported as [SmsAccessState.PERMANENTLY_DENIED] too. App settings
     * can still grant the permission in that case, so the user is never
     * left without a way forward.
     */
    fun stateFor(granted: Boolean, requestedBefore: Boolean, shouldShowRationale: Boolean): SmsAccessState = when {
        granted -> SmsAccessState.GRANTED
        requestedBefore && !shouldShowRationale -> SmsAccessState.PERMANENTLY_DENIED
        else -> SmsAccessState.CAN_REQUEST
    }

    /**
     * Reads the facts for this app right now and applies [stateFor].
     * Android's rationale answer needs an [activity]; without one it counts
     * as false.
     */
    fun currentState(
        context: Context,
        activity: Activity?,
        requestStore: SmsPermissionRequestStore
    ): SmsAccessState = stateFor(
        granted = SmsPermissions.areGranted(context),
        requestedBefore = requestStore.wasRequested(),
        shouldShowRationale = activity != null && SmsPermissions.shouldShowRationale(activity)
    )

    /** The system screen with the details of the app [packageName], where its permissions can be changed. */
    fun appSettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(PACKAGE_SCHEME, packageName, null))
}
