package com.sajib.smsexpensetracker.domain.model

/** Outcome of saving a confirmed shared or pasted transaction. */
sealed interface ManualEntrySaveResult {

    /** A new transaction row was written. */
    data object Saved : ManualEntrySaveResult

    /** An identical transaction, including its date and time, is already stored. Nothing was written. */
    data object Duplicate : ManualEntrySaveResult
}
