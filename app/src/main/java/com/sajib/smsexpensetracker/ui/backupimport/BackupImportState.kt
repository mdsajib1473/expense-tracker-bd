package com.sajib.smsexpensetracker.ui.backupimport

import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.ImportProgress
import com.sajib.smsexpensetracker.domain.model.ImportSummary

/** What the backup import UI shows. */
sealed interface BackupImportState {

    /** No import running and nothing to report. */
    data object Idle : BackupImportState

    /** An import is running. */
    data class Importing(val progress: ImportProgress) : BackupImportState

    /** The import finished; [summary] holds counts only. */
    data class Done(val summary: ImportSummary) : BackupImportState

    /** The import stopped early; rows saved before that stay saved. */
    data class Failed(val error: BackupImportError) : BackupImportState
}
