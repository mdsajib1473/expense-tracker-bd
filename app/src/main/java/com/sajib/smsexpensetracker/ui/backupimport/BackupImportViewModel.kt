package com.sajib.smsexpensetracker.ui.backupimport

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajib.smsexpensetracker.data.backup.BackupFileOpener
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import com.sajib.smsexpensetracker.domain.model.ImportProgress
import com.sajib.smsexpensetracker.domain.usecase.ImportSmsBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Drives importing an SMS Backup & Restore file picked by the user. Logs
 * only the summary counts or the failure category, never message content.
 */
@HiltViewModel
class BackupImportViewModel @Inject constructor(
    private val fileOpener: BackupFileOpener,
    private val importSmsBackup: ImportSmsBackupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<BackupImportState>(BackupImportState.Idle)

    /** Current import state for the UI. */
    val state: StateFlow<BackupImportState> = _state.asStateFlow()

    private var importJob: Job? = null

    /** Imports the backup file at [uri]. Ignored while an import is already running. */
    fun import(uri: Uri) {
        if (importJob?.isActive == true) return
        _state.value = BackupImportState.Importing(ImportProgress(processed = 0, totalHint = null))
        importJob = viewModelScope.launch {
            val result = try {
                val summary = fileOpener.read(uri) { stream ->
                    importSmsBackup(stream) { progress ->
                        _state.update { current ->
                            if (current is BackupImportState.Importing) BackupImportState.Importing(progress) else current
                        }
                    }
                }
                Log.i(TAG, "Import finished: $summary")
                BackupImportState.Done(summary)
            } catch (e: BackupImportException) {
                Log.w(TAG, "Import failed: ${e.message}")
                BackupImportState.Failed(e.error)
            } catch (e: IOException) {
                // Reads are already mapped to BackupImportException; this is closing the file.
                Log.w(TAG, "Import failed: ${BackupImportError.READ_FAILED}")
                BackupImportState.Failed(BackupImportError.READ_FAILED)
            }
            _state.value = result
        }
    }

    /** Stops a running import. Transactions saved so far stay saved. */
    fun cancel() {
        importJob?.cancel()
        importJob = null
        _state.value = BackupImportState.Idle
    }

    /** Clears a finished or failed result once the user has seen it. */
    fun dismiss() {
        if (importJob?.isActive != true) _state.value = BackupImportState.Idle
    }

    private companion object {
        const val TAG = "BackupImport"
    }
}
