package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionLookup
import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.parser.core.BodyIdentification
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import javax.inject.Inject

/**
 * Checks one shared or pasted message, which arrives with no sender address
 * and no timestamp, and turns it into a [ManualEntryPreparation.Preview] for
 * the user to confirm. Nothing is written here.
 *
 * The text is untrusted input: it is rejected before parsing when it is
 * longer than [MAX_TEXT_LENGTH] or blank, and identified with
 * [ParserEngine.identifyByBody], which accepts it only when exactly one
 * provider recognizes it. The text itself is never stored, logged or
 * returned (AGENT.md rule 12); this class does no logging at all.
 */
class PrepareManualEntryUseCase @Inject constructor(
    private val parserEngine: ParserEngine,
    private val lookup: TransactionLookup
) {

    /** Checks [text]. The parse timestamp is irrelevant here, since parsers read only the body. */
    suspend operator fun invoke(text: String): ManualEntryPreparation {
        if (text.length > MAX_TEXT_LENGTH) return ManualEntryPreparation.TooLong
        if (text.isBlank()) return ManualEntryPreparation.Blank

        return when (val identification = parserEngine.identifyByBody(text, receivedAt = 0L)) {
            BodyIdentification.NoMatch -> ManualEntryPreparation.NoMatch
            BodyIdentification.Ambiguous -> ManualEntryPreparation.Ambiguous
            is BodyIdentification.Matched -> {
                val result = identification.result
                ManualEntryPreparation.Preview(
                    institutionName = identification.institutionName,
                    type = result.type,
                    amount = result.amount,
                    balance = result.balance,
                    reference = result.reference,
                    similarExists = lookup.hasSimilar(
                        institutionName = identification.institutionName,
                        type = result.type,
                        amount = result.amount,
                        balance = result.balance
                    )
                )
            }
        }
    }

    companion object {
        /**
         * Longest accepted text, in UTF-16 characters. A real bank or wallet
         * message, even a multi-part one with a promotional prefix, is far
         * shorter; anything longer is not one message.
         */
        const val MAX_TEXT_LENGTH = 2000
    }
}
