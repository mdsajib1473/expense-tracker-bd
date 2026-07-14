package com.sajib.smsexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import com.sajib.smsexpensetracker.importer.SmsImportJob
import com.sajib.smsexpensetracker.ui.common.SmsPermissionGate
import com.sajib.smsexpensetracker.ui.transactions.TransactionListScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host for the Compose UI. At M1 it gates everything behind
 * the SMS permission flow (rationale dialog first, per AGENT.md Hard
 * Constraint 2), then shows the minimal transaction list. On the first launch
 * where permission is granted, it also kicks off the one-time historical SMS
 * import. Real dashboard styling arrives in M3.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var importJob: SmsImportJob
    @Inject lateinit var firstLaunchTracker: FirstLaunchTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SmsPermissionGate(
                    onPermissionGranted = {
                        if (!firstLaunchTracker.hasImportedHistory()) {
                            lifecycleScope.launch {
                                importJob.run()
                                firstLaunchTracker.markHistoryImported()
                            }
                        }
                    }
                ) {
                    TransactionListScreen()
                }
            }
        }
    }
}
