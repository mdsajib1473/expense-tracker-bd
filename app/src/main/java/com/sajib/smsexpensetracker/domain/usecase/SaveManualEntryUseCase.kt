package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult
import com.sajib.smsexpensetracker.parser.core.ParseResult
import com.sajib.smsexpensetracker.parser.core.ParsedTransaction
import javax.inject.Inject

/**
 * Saves a shared or pasted transaction the user confirmed, through the same
 * duplicate check as every other source ([TransactionWriter.saveIfNew]).
 *
 * Only the previewed fields are stored. The message text was dropped when
 * the preview was made, so the row's rawSms is [NO_MESSAGE_TEXT] and its
 * counterparty is null.
 */
class SaveManualEntryUseCase @Inject constructor(
    private val writer: TransactionWriter
) {

    /** Saves [preview] as received at [receivedAt] (epoch millis, confirmed by the user). */
    suspend operator fun invoke(
        preview: ManualEntryPreparation.Preview,
        receivedAt: Long
    ): ManualEntrySaveResult {
        val parsed = ParsedTransaction(
            institutionName = preview.institutionName,
            result = ParseResult(
                type = preview.type,
                amount = preview.amount,
                balance = preview.balance,
                counterparty = null,
                reference = preview.reference,
                rawSms = NO_MESSAGE_TEXT
            )
        )
        return if (writer.saveIfNew(parsed, receivedAt)) {
            ManualEntrySaveResult.Saved
        } else {
            ManualEntrySaveResult.Duplicate
        }
    }

    companion object {
        /** Stored as rawSms for share and paste intake, which never keeps the message text. */
        const val NO_MESSAGE_TEXT = ""
    }
}
