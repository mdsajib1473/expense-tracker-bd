package com.sajib.smsexpensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.sajib.smsexpensetracker.capture.LiveCaptureSetup
import com.sajib.smsexpensetracker.ui.main.AppRoot
import com.sajib.smsexpensetracker.ui.manualentry.ManualEntryViewModel
import com.sajib.smsexpensetracker.ui.manualentry.SharedTextIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host for the Compose UI, and the share target for one
 * text/plain message from another app (launchMode singleTop: a share while
 * this activity is already on top arrives through [onNewIntent]).
 *
 * Shared text goes straight to [ManualEntryViewModel] and nowhere else.
 * The consumed intent is then replaced, and the launch intent is ignored
 * after recreation or a relaunch from Recents, so the same message is never
 * processed twice. Live capture is flavor-specific and never gates the UI;
 * it is (re)checked on every resume so the one-time inbox import starts as
 * soon as the full flavor's SMS permission is granted. Real dashboard
 * styling arrives in M3.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var liveCaptureSetup: LiveCaptureSetup

    private val manualEntryViewModel: ManualEntryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SharedTextIntent.isFreshLaunch(intent, isRecreated = savedInstanceState != null)) {
            consumeShare(intent)
        }
        setContent {
            MaterialTheme {
                AppRoot(
                    liveCaptureSetup = liveCaptureSetup,
                    manualEntryViewModel = manualEntryViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeShare(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { liveCaptureSetup.startCapture() }
    }

    private fun consumeShare(intent: Intent) {
        val text = SharedTextIntent.extractText(intent) ?: return
        setIntent(Intent(this, MainActivity::class.java))
        manualEntryViewModel.onSharedText(text)
    }
}
