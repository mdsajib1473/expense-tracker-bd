package com.sajib.smsexpensetracker.domain.model

/**
 * Outcome of handing one SMS to the ingestion entry point.
 *
 * Never carries the message body, the sender address or any parsed field
 * beyond the institution name, so a result can be logged or shown without
 * leaking SMS content (AGENT.md Hard Constraint 1, rules 8 and 12).
 */
sealed interface IngestResult {

    /** The SMS was recognized and a new transaction row was written. */
    data class Saved(val institutionName: String) : IngestResult

    /** The SMS was recognized but an identical transaction is already stored. */
    data class Duplicate(val institutionName: String) : IngestResult

    /** No parser recognized the SMS. Nothing was stored. */
    data object Unrecognized : IngestResult
}
