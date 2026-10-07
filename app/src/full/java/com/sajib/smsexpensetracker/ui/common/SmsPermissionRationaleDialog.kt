package com.sajib.smsexpensetracker.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sajib.smsexpensetracker.R

/**
 * Full flavor only. Shown before the real OS SMS permission prompt, per
 * AGENT.md Hard Constraint 2. Only when the user taps Continue does the caller actually
 * launch the system permission request; this composable never touches the
 * permission system itself.
 */
@Composable
fun SmsPermissionRationaleDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sms_permission_rationale_title)) },
        text = { Text(stringResource(R.string.sms_permission_rationale_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.sms_permission_rationale_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sms_permission_rationale_dismiss))
            }
        }
    )
}
