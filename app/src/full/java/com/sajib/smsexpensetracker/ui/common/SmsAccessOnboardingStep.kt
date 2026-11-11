package com.sajib.smsexpensetracker.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.capture.SmsAccessPolicy
import com.sajib.smsexpensetracker.capture.SmsAccessState
import com.sajib.smsexpensetracker.capture.SmsPermissionRequestStore
import com.sajib.smsexpensetracker.capture.SmsPermissions

private const val TAG = "SmsAccessStep"

/**
 * Full flavor only. The optional onboarding step "Read new messages
 * automatically". What it offers is decided by [SmsAccessPolicy], not here:
 *
 * - While Android will still show its prompt, a button opens the rationale
 *   dialog first (Hard Constraint 2), and only its Continue button launches
 *   the OS permission prompt.
 * - Once Android no longer shows its prompt, the button reads "Open app
 *   settings" and opens this app's system settings screen, the only place
 *   left where the permission can be granted.
 * - Once both permissions are granted, a confirmation replaces the button.
 *
 * Declining, dismissing or never tapping changes nothing else: the rest of
 * the app works without the permission. The state is read again on every
 * resume, because a change made in app settings reports nothing back, and
 * the activity's resume also starts the one-time inbox import once the
 * permissions are granted.
 */
@Composable
fun SmsAccessOnboardingStep() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val requestStore = remember(context) { SmsPermissionRequestStore(context.applicationContext) }
    var state by remember { mutableStateOf(SmsAccessPolicy.currentState(context, activity, requestStore)) }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestStore.markRequested()
        state = SmsAccessPolicy.currentState(context, activity, requestStore)
    }

    LifecycleResumeEffect(context, activity) {
        state = SmsAccessPolicy.currentState(context, activity, requestStore)
        onPauseOrDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
        Text(
            text = stringResource(R.string.live_capture_onboarding_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.live_capture_onboarding_body),
            style = MaterialTheme.typography.bodyLarge
        )
        when (state) {
            SmsAccessState.GRANTED -> Text(
                text = stringResource(R.string.live_capture_onboarding_granted),
                style = MaterialTheme.typography.bodyLarge
            )
            SmsAccessState.CAN_REQUEST -> Button(onClick = { showRationale = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sms_permission_grant_button))
            }
            SmsAccessState.PERMANENTLY_DENIED -> {
                Text(
                    text = stringResource(R.string.live_capture_onboarding_denied),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = { openAppSettings(activity ?: context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sms_permission_open_settings_button))
                }
            }
        }
    }

    if (showRationale) {
        SmsPermissionRationaleDialog(
            onContinue = {
                showRationale = false
                launcher.launch(SmsPermissions.REQUIRED)
            },
            onDismiss = { showRationale = false }
        )
    }
}

/** Opens this app's system settings screen. A device without that screen is logged and otherwise ignored. */
private fun openAppSettings(context: Context) {
    try {
        context.startActivity(SmsAccessPolicy.appSettingsIntent(context.packageName))
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "App settings screen is not available")
    }
}
