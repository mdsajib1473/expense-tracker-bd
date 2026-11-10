package com.sajib.smsexpensetracker.ui.manualentry

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.domain.usecase.PrepareManualEntryUseCase
import com.sajib.smsexpensetracker.domain.usecase.SaveManualEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * Drives adding one transaction from a shared or pasted message:
 * Idle, Input (paste field open), Checking, then Preview or Error, then
 * Saved after the user confirms.
 *
 * The message text is untrusted and private. It lives only in this
 * ViewModel's memory ([draft] while the paste field is open, and the
 * running check), never in SavedStateHandle, saved instance state, a log or
 * an intent result. It is dropped as soon as a check starts and when the
 * dialog is dismissed. Logs hold only the outcome and the institution name
 * (AGENT.md rule 8).
 */
@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val prepareManualEntry: PrepareManualEntryUseCase,
    private val saveManualEntry: SaveManualEntryUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow<ManualEntryState>(ManualEntryState.Idle)

    /** Current intake state for the UI. */
    val state: StateFlow<ManualEntryState> = _state.asStateFlow()

    private val _draft = MutableStateFlow("")

    /** Text in the paste field. Memory only; empty whenever the field is closed. */
    val draft: StateFlow<String> = _draft.asStateFlow()

    /** Time zone the transaction date and time are shown and edited in. */
    val zone: ZoneId get() = clock.zone

    /** Today as the UTC midnight the date picker works in; later dates cannot be picked. */
    fun latestPickableDateMillis(): Long = ReceivedAtTime.pickerDateMillis(clock.millis(), zone)

    private var job: Job? = null

    /** Opens an empty paste field, replacing whatever was shown. */
    fun openPaste() {
        stopWork()
        _state.value = ManualEntryState.Input
    }

    /**
     * Updates the paste field. Text beyond one character over the limit is
     * cut off here so a huge paste is never held, and is still rejected as
     * too long by the check.
     */
    fun onDraftChange(text: String) {
        if (_state.value != ManualEntryState.Input) return
        _draft.value = text.take(PrepareManualEntryUseCase.MAX_TEXT_LENGTH + 1)
    }

    /** Checks the pasted text and drops it from the paste field. Ignored unless the field is open. */
    fun check() {
        if (_state.value != ManualEntryState.Input) return
        val text = _draft.value
        _draft.value = ""
        startCheck(text)
    }

    /** Checks text shared from another app, replacing whatever was shown. */
    fun onSharedText(text: String) {
        stopWork()
        startCheck(text)
    }

    /** Moves the transaction to the date picked as [pickedUtcMillis] (a UTC midnight), keeping its time. */
    fun onDateSelected(pickedUtcMillis: Long) {
        val current = _state.value as? ManualEntryState.Preview ?: return
        if (current.isSaving) return
        _state.value = current.copy(
            receivedAt = ReceivedAtTime.withPickedDate(current.receivedAt, pickedUtcMillis, zone)
        )
    }

    /** Moves the transaction to [hour]:[minute] on its current date. */
    fun onTimeSelected(hour: Int, minute: Int) {
        val current = _state.value as? ManualEntryState.Preview ?: return
        if (current.isSaving) return
        _state.value = current.copy(
            receivedAt = ReceivedAtTime.withPickedTime(current.receivedAt, hour, minute, zone)
        )
    }

    /** Saves the previewed transaction at the confirmed date and time. Ignored while a save runs. */
    fun save() {
        val current = _state.value as? ManualEntryState.Preview ?: return
        if (current.isSaving) return
        _state.value = current.copy(isSaving = true)
        job = viewModelScope.launch {
            val result = saveManualEntry(current.preview, current.receivedAt)
            Log.i(TAG, "Save: $result, ${current.preview.institutionName}")
            _state.value = ManualEntryState.Saved(result)
        }
    }

    /** Closes whatever is shown and drops any text. */
    fun dismiss() {
        stopWork()
        _state.value = ManualEntryState.Idle
    }

    private fun startCheck(text: String) {
        _state.value = ManualEntryState.Checking
        job = viewModelScope.launch {
            val next = when (val preparation = prepareManualEntry(text)) {
                is ManualEntryPreparation.Preview ->
                    ManualEntryState.Preview(preparation, ReceivedAtTime.toMinute(clock.millis()))
                ManualEntryPreparation.NoMatch -> ManualEntryState.Error(ManualEntryError.NOT_RECOGNIZED)
                ManualEntryPreparation.Ambiguous -> ManualEntryState.Error(ManualEntryError.AMBIGUOUS)
                ManualEntryPreparation.TooLong -> ManualEntryState.Error(ManualEntryError.TOO_LONG)
                ManualEntryPreparation.Blank -> ManualEntryState.Error(ManualEntryError.BLANK)
            }
            if (next is ManualEntryState.Preview) {
                Log.i(TAG, "Check: matched ${next.preview.institutionName}")
            } else {
                Log.i(TAG, "Check: $next")
            }
            _state.value = next
        }
    }

    private fun stopWork() {
        job?.cancel()
        job = null
        _draft.value = ""
    }

    private companion object {
        const val TAG = "ManualEntry"
    }
}
