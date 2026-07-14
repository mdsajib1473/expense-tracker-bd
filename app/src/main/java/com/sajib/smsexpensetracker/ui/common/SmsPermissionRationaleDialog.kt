package com.sajib.smsexpensetracker.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Shown before the real OS SMS permission prompt, per AGENT.md Hard
 * Constraint 2. Only when the user taps Continue does the caller actually
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
        title = { Text("SMS Access Needed / এসএমএস অ্যাক্সেস প্রয়োজন") },
        text = {
            Text(
                "This app reads transaction SMS from bKash, Nagad, Rocket, " +
                    "and your banks to build your expense dashboard. " +
                    "Messages never leave your device.\n\n" +
                    "এই অ্যাপটি আপনার বিকাশ, নগদ, রকেট এবং ব্যাংকের লেনদেনের " +
                    "এসএমএস পড়ে খরচের ড্যাশবোর্ড তৈরি করে। মেসেজ কখনো আপনার " +
                    "ডিভাইস থেকে বাইরে যায় না।"
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continue / এগিয়ে যান") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now / এখন না") }
        }
    )
}
