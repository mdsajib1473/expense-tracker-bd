package com.sajib.smsexpensetracker.ui.backupimport

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.ImportProgress
import com.sajib.smsexpensetracker.domain.model.ImportSummary

/**
 * Shows the running import, its summary or its failure as a dialog; shows
 * nothing while [state] is Idle. [onCancel] stops a running import,
 * [onDismiss] closes a summary or failure.
 */
@Composable
fun BackupImportDialog(
    state: BackupImportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        BackupImportState.Idle -> Unit
        is BackupImportState.Importing -> ImportingDialog(state.progress, onCancel)
        is BackupImportState.Done -> ResultDialog(
            title = stringResource(R.string.backup_import_done_title),
            message = summaryText(state.summary),
            onDismiss = onDismiss
        )
        is BackupImportState.Failed -> ResultDialog(
            title = stringResource(R.string.backup_import_failed_title),
            message = stringResource(errorMessage(state.error)),
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ImportingDialog(progress: ImportProgress, onCancel: () -> Unit) {
    val total = progress.totalHint?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.backup_import_progress_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (total == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(pluralStringResource(R.plurals.backup_import_progress, progress.processed, progress.processed))
                } else {
                    LinearProgressIndicator(
                        progress = { (progress.processed.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.backup_import_progress_of_total,
                            total,
                            progress.processed,
                            total
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.backup_import_cancel)) }
        }
    )
}

@Composable
private fun ResultDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.backup_import_ok)) }
        }
    )
}

@Composable
private fun summaryText(summary: ImportSummary): String {
    val sentences = buildList {
        add(pluralStringResource(R.plurals.backup_import_read, summary.totalRead, summary.totalRead))
        add(pluralStringResource(R.plurals.backup_import_saved, summary.saved, summary.saved))
        add(pluralStringResource(R.plurals.backup_import_duplicates, summary.duplicates, summary.duplicates))
        add(pluralStringResource(R.plurals.backup_import_unrecognized, summary.unrecognized, summary.unrecognized))
        if (summary.skippedNotInbox > 0) {
            add(pluralStringResource(R.plurals.backup_import_not_inbox, summary.skippedNotInbox, summary.skippedNotInbox))
        }
        if (summary.invalid > 0) {
            add(pluralStringResource(R.plurals.backup_import_invalid, summary.invalid, summary.invalid))
        }
    }
    return sentences.joinToString(separator = " ")
}

@StringRes
private fun errorMessage(error: BackupImportError): Int = when (error) {
    BackupImportError.NOT_SMS_BACKUP -> R.string.backup_import_error_not_backup
    BackupImportError.MALFORMED -> R.string.backup_import_error_malformed
    BackupImportError.READ_FAILED -> R.string.backup_import_error_read_failed
}
