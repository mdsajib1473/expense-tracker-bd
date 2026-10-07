package com.sajib.smsexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.sajib.smsexpensetracker.capture.LiveCaptureSetup
import com.sajib.smsexpensetracker.ui.transactions.TransactionListScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host for the Compose UI. Live capture is flavor-specific:
 * in the full flavor the content is gated behind the SMS permission flow and
 * the one-time inbox import starts once permission is granted; in the play
 * flavor the content shows directly and no capture starts. Real dashboard
 * styling arrives in M3.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var liveCaptureSetup: LiveCaptureSetup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                liveCaptureSetup.PermissionGate(
                    onReady = { lifecycleScope.launch { liveCaptureSetup.startCapture() } }
                ) {
                    TransactionListScreen()
                }
            }
        }
    }
}
