package com.sajib.smsexpensetracker.ui.common

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
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.capture.SmsPermissions

/**
 * Full flavor only. The optional onboarding step "Read new messages
 * automatically". Tapping the button shows the rationale dialog first (Hard
 * Constraint 2) and only its Continue button launches the OS permission
 * prompt. Declining, dismissing or never tapping changes nothing else: the
 * rest of the app works without the permission. Once both permissions are
 * granted, the activity's next resume starts the one-time inbox import.
 */
@Composable
fun SmsAccessOnboardingStep() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(SmsPermissions.areGranted(context)) }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = SmsPermissions.areGranted(context)
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
        if (granted) {
            Text(
                text = stringResource(R.string.live_capture_onboarding_granted),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Button(onClick = { showRationale = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sms_permission_grant_button))
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
