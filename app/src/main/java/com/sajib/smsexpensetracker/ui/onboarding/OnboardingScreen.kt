package com.sajib.smsexpensetracker.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.sajib.smsexpensetracker.R

/**
 * The onboarding screens: what the app does and the privacy statement, then
 * the three ways to add transactions, then, when [liveCaptureStep] is given
 * (full flavor), the optional step that turns on reading new messages
 * automatically. Every step can be skipped; [onClose] is called on Skip and
 * on Done.
 */
@Composable
fun OnboardingScreen(
    onClose: () -> Unit,
    liveCaptureStep: (@Composable () -> Unit)?
) {
    val stepCount = if (liveCaptureStep == null) BASE_STEP_COUNT else BASE_STEP_COUNT + 1
    var step by rememberSaveable { mutableIntStateOf(0) }
    val isLast = step == stepCount - 1

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(dimensionResource(R.dimen.spacing_xlarge))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step_count, step + 1, stepCount),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (!isLast) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = dimensionResource(R.dimen.spacing_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> WaysStep()
                    else -> liveCaptureStep?.invoke()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                Button(
                    onClick = { if (isLast) onClose() else step++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (isLast) R.string.onboarding_done else R.string.onboarding_next))
                }
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepTitle(R.string.onboarding_welcome_title)
    Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.privacy_statement), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun WaysStep() {
    StepTitle(R.string.onboarding_ways_title)
    Way(R.string.onboarding_way_import_title, R.string.onboarding_way_import_body)
    Way(R.string.onboarding_way_share_title, R.string.onboarding_way_share_body)
    Way(R.string.onboarding_way_paste_title, R.string.onboarding_way_paste_body)
}

@Composable
private fun StepTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun Way(@StringRes title: Int, @StringRes body: Int) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Welcome and privacy, then the three ways to add transactions. */
private const val BASE_STEP_COUNT = 2
