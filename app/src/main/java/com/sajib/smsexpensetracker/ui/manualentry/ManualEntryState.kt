package com.sajib.smsexpensetracker.ui.manualentry

import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult

/**
 * What the share and paste intake UI shows. No state holds the message
 * text: while the paste field is open the text lives only in
 * [ManualEntryViewModel.draft], so a state can be logged or printed safely.
 */
sealed interface ManualEntryState {

    /** Nothing is shown. */
    data object Idle : ManualEntryState

    /** The paste field is open. */
    data object Input : ManualEntryState

    /** A message is being checked. */
    data object Checking : ManualEntryState

    /**
     * A message was recognized. [receivedAt] (epoch millis) is the date and
     * time the user confirms, which defaults to now. [isSaving] is true while
     * the save runs.
     */
    data class Preview(
        val preview: ManualEntryPreparation.Preview,
        val receivedAt: Long,
        val isSaving: Boolean = false
    ) : ManualEntryState

    /** The message was not accepted. Nothing was saved. */
    data class Error(val reason: ManualEntryError) : ManualEntryState

    /** The confirmed transaction was saved, or was already stored. */
    data class Saved(val result: ManualEntrySaveResult) : ManualEntryState
}

/** Why a shared or pasted message was not accepted. */
enum class ManualEntryError {

    /** No supported provider recognized it. */
    NOT_RECOGNIZED,

    /** More than one provider recognized it. */
    AMBIGUOUS,

    /** It is longer than one message can be. */
    TOO_LONG,

    /** It was empty or whitespace only. */
    BLANK
}
