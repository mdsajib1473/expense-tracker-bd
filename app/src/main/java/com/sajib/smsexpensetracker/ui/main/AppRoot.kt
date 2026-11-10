package com.sajib.smsexpensetracker.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sajib.smsexpensetracker.capture.LiveCaptureSetup
import com.sajib.smsexpensetracker.ui.manualentry.ManualEntryDialog
import com.sajib.smsexpensetracker.ui.manualentry.ManualEntryViewModel
import com.sajib.smsexpensetracker.ui.onboarding.OnboardingScreen
import com.sajib.smsexpensetracker.ui.onboarding.OnboardingViewModel
import com.sajib.smsexpensetracker.ui.transactions.TransactionListScreen

/**
 * Top-level content: the onboarding screens while they are visible,
 * otherwise the transaction list. The share and paste intake dialog sits
 * above both, so a message shared on first launch is handled right away.
 * Nothing here is gated on the SMS permission.
 */
@Composable
fun AppRoot(
    liveCaptureSetup: LiveCaptureSetup,
    manualEntryViewModel: ManualEntryViewModel,
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val showOnboarding by onboardingViewModel.visible.collectAsState()

    if (showOnboarding) {
        BackHandler(onBack = onboardingViewModel::close)
        OnboardingScreen(
            onClose = onboardingViewModel::close,
            liveCaptureStep = if (liveCaptureSetup.hasOnboardingStep) {
                { liveCaptureSetup.OnboardingStep() }
            } else {
                null
            }
        )
    } else {
        TransactionListScreen(
            onAddFromMessage = manualEntryViewModel::openPaste,
            onShowHowToAdd = onboardingViewModel::show
        )
    }

    ManualEntryDialog(manualEntryViewModel)
}
