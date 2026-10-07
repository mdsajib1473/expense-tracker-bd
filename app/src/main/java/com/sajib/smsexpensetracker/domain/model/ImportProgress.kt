package com.sajib.smsexpensetracker.domain.model

/**
 * Progress of a running backup import: [processed] messages so far, out of
 * [totalHint] if the file declared a count. The hint comes from the file
 * itself and is only shown, never trusted for control flow.
 */
data class ImportProgress(
    val processed: Int,
    val totalHint: Int?
)
