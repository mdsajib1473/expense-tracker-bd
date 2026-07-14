package com.sajib.smsexpensetracker.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Shows the rationale dialog first (Hard Constraint 2), then the real OS
 * permission prompt only after the user taps Continue. Calls
 * [onPermissionGranted] once, whenever permission becomes granted, whether
 * that is immediately (already granted from a previous launch) or after the
 * user grants it now. The caller decides what to actually do on grant, this
 * composable only reports the event.
 */
@Composable
fun SmsPermissionGate(
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    fun hasPermissions() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasPermissions()) }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
    }

    LaunchedEffect(granted) {
        if (granted) onPermissionGranted()
    }

    if (granted) {
        content()
        return
    }

    if (showRationale) {
        SmsPermissionRationaleDialog(
            onContinue = {
                showRationale = false
                launcher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            },
            onDismiss = { showRationale = false }
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { showRationale = true }) {
            Text("Grant SMS Access")
        }
    }
}
