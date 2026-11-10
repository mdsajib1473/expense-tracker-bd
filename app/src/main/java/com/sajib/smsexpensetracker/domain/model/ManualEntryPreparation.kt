package com.sajib.smsexpensetracker.domain.model

import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Outcome of checking one shared or pasted message before it is saved.
 *
 * No variant holds the message text, so a result can be kept in UI state,
 * logged or printed without leaking an OTP or a PIN (AGENT.md Hard
 * Constraint 1, rule 12). [Preview] holds only the parsed fields that are
 * shown to the user for confirmation.
 */
sealed interface ManualEntryPreparation {

    /**
     * Exactly one provider recognized the message. [similarExists] is true
     * when a stored transaction already has the same institution, type,
     * amount and balance at any time, so the user is warned before saving.
     */
    data class Preview(
        val institutionName: String,
        val type: TransactionType,
        val amount: BigDecimal,
        val balance: BigDecimal?,
        val reference: String?,
        val similarExists: Boolean
    ) : ManualEntryPreparation

    /** No supported provider recognized the message. Nothing was kept. */
    data object NoMatch : ManualEntryPreparation

    /** More than one provider recognized the message, so it is not accepted. */
    data object Ambiguous : ManualEntryPreparation

    /** The text is longer than one message can be and was rejected before parsing. */
    data object TooLong : ManualEntryPreparation

    /** The text is empty or whitespace only. */
    data object Blank : ManualEntryPreparation
}
