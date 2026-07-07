package com.sajib.smsexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Single-activity host for the Compose UI. At M0 it renders a placeholder
 * screen only; real screens (dashboard, transactions, reports, settings)
 * arrive in later milestones under ui/.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PlaceholderScreen()
            }
        }
    }
}

/**
 * Placeholder content proving the Compose toolchain works end to end.
 * Replaced by the dashboard screen in M3.
 */
@Composable
private fun PlaceholderScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Text(text = stringResource(R.string.app_name))
    }
}
