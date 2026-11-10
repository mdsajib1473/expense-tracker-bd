package com.sajib.smsexpensetracker.ui.manualentry

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.DialogProperties
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult
import com.sajib.smsexpensetracker.domain.usecase.PrepareManualEntryUseCase
import com.sajib.smsexpensetracker.ui.common.labelRes
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shows the share and paste intake for [viewModel]'s current state: the
 * paste field, the check in progress, the preview to confirm, an error, or
 * the saved result. Shows nothing while Idle. No dialog ever displays the
 * message text other than the paste field the user is typing into.
 */
@Composable
fun ManualEntryDialog(viewModel: ManualEntryViewModel) {
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        ManualEntryState.Idle -> Unit
        ManualEntryState.Input -> {
            val draft by viewModel.draft.collectAsState()
            PasteDialog(
                text = draft,
                onTextChange = viewModel::onDraftChange,
                onCheck = viewModel::check,
                onCancel = viewModel::dismiss
            )
        }
        ManualEntryState.Checking -> CheckingDialog(onCancel = viewModel::dismiss)
        is ManualEntryState.Preview -> PreviewDialog(
            state = current,
            zone = viewModel.zone,
            latestPickableDateMillis = viewModel.latestPickableDateMillis(),
            onDateSelected = viewModel::onDateSelected,
            onTimeSelected = viewModel::onTimeSelected,
            onSave = viewModel::save,
            onCancel = viewModel::dismiss
        )
        is ManualEntryState.Error -> MessageDialog(
            title = stringResource(R.string.manual_entry_error_title),
            message = errorMessage(current.reason),
            onClose = viewModel::dismiss,
            onTryAgain = viewModel::openPaste
        )
        is ManualEntryState.Saved -> when (current.result) {
            ManualEntrySaveResult.Saved -> MessageDialog(
                title = stringResource(R.string.manual_entry_saved_title),
                message = stringResource(R.string.manual_entry_saved_body),
                onClose = viewModel::dismiss
            )
            ManualEntrySaveResult.Duplicate -> MessageDialog(
                title = stringResource(R.string.manual_entry_duplicate_title),
                message = stringResource(R.string.manual_entry_duplicate_body),
                onClose = viewModel::dismiss
            )
        }
    }
}

@Composable
private fun PasteDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onCheck: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.manual_entry_input_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                Text(stringResource(R.string.manual_entry_input_help), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(R.string.manual_entry_input_label)) },
                    minLines = PASTE_FIELD_MIN_LINES,
                    maxLines = PASTE_FIELD_MAX_LINES,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCheck) { Text(stringResource(R.string.manual_entry_check)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.manual_entry_cancel)) }
        }
    )
}

@Composable
private fun CheckingDialog(onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.manual_entry_input_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.manual_entry_checking))
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.manual_entry_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewDialog(
    state: ManualEntryState.Preview,
    zone: ZoneId,
    latestPickableDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val preview = state.preview
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.manual_entry_preview_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                LabeledValue(R.string.manual_entry_field_institution, preview.institutionName)
                LabeledValue(R.string.manual_entry_field_type, stringResource(preview.type.labelRes()))
                LabeledValue(R.string.manual_entry_field_amount, amountText(preview.amount))
                LabeledValue(R.string.manual_entry_field_balance, preview.balance?.let { amountText(it) } ?: missingText())
                LabeledValue(R.string.manual_entry_field_reference, preview.reference ?: missingText())
                LabeledValue(R.string.manual_entry_field_date, dateTimeText(state.receivedAt, zone))
                Text(stringResource(R.string.manual_entry_date_help), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showDatePicker = true }, enabled = !state.isSaving) {
                    Text(stringResource(R.string.manual_entry_change_date))
                }
                TextButton(onClick = { showTimePicker = true }, enabled = !state.isSaving) {
                    Text(stringResource(R.string.manual_entry_change_time))
                }
                if (preview.similarExists) {
                    Text(
                        text = stringResource(R.string.manual_entry_similar_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving) {
                Text(
                    stringResource(
                        if (preview.similarExists) R.string.manual_entry_add_anyway else R.string.manual_entry_save
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.manual_entry_cancel)) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = ReceivedAtTime.pickerDateMillis(state.receivedAt, zone),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= latestPickableDateMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    showDatePicker = false
                }) { Text(stringResource(R.string.manual_entry_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.manual_entry_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val time = ReceivedAtTime.localTime(state.receivedAt, zone)
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.manual_entry_time_picker_title)) },
            text = { TimeInput(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.manual_entry_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.manual_entry_cancel)) }
            }
        )
    }
}

@Composable
private fun LabeledValue(@StringRes label: Int, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MessageDialog(
    title: String,
    message: String,
    onClose: () -> Unit,
    onTryAgain: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Text(message, modifier = Modifier.verticalScroll(rememberScrollState()))
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.manual_entry_close)) }
        },
        dismissButton = onTryAgain?.let { tryAgain ->
            { TextButton(onClick = tryAgain) { Text(stringResource(R.string.manual_entry_try_again)) } }
        }
    )
}

@Composable
private fun amountText(amount: BigDecimal): String =
    stringResource(R.string.manual_entry_amount, amount.toPlainString())

@Composable
private fun missingText(): String = stringResource(R.string.manual_entry_value_missing)

private fun dateTimeText(epochMillis: Long, zone: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(zone)
        .format(Instant.ofEpochMilli(epochMillis))

@Composable
private fun errorMessage(reason: ManualEntryError): String = when (reason) {
    ManualEntryError.NOT_RECOGNIZED -> stringResource(R.string.manual_entry_error_not_recognized)
    ManualEntryError.AMBIGUOUS -> stringResource(R.string.manual_entry_error_ambiguous)
    ManualEntryError.TOO_LONG -> pluralStringResource(
        R.plurals.manual_entry_error_too_long,
        PrepareManualEntryUseCase.MAX_TEXT_LENGTH,
        PrepareManualEntryUseCase.MAX_TEXT_LENGTH
    )
    ManualEntryError.BLANK -> stringResource(R.string.manual_entry_error_blank)
}

private const val PASTE_FIELD_MIN_LINES = 4
private const val PASTE_FIELD_MAX_LINES = 10
