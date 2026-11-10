package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.repository.TransactionWriter
import com.sajib.smsexpensetracker.domain.model.IngestResult
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import javax.inject.Inject

/**
 * The single ingestion entry point for every SMS that arrives with its
 * sender address and timestamp (live receiver, inbox import, backup file
 * import), so sender-routed parsing, de-duplication and saving exist in
 * exactly one place. Shared or pasted text has neither, so it goes through
 * PrepareManualEntryUseCase and SaveManualEntryUseCase instead, which use
 * the same parsers and the same duplicate check.
 *
 * An unrecognized body is dropped immediately: it is never stored, never
 * logged and never returned, since it may contain an OTP or a PIN
 * (AGENT.md rule 12). This class does no logging at all; callers may log the
 * returned [IngestResult], which holds no body.
 */
class IngestSmsUseCase @Inject constructor(
    private val parserEngine: ParserEngine,
    private val writer: TransactionWriter
) {

    /**
     * Parses [body] from [sender], received at [receivedAt] (epoch millis from
     * the SMS itself), and stores it if it is a new transaction.
     */
    suspend operator fun invoke(sender: String, body: String, receivedAt: Long): IngestResult {
        val parsed = parserEngine.parse(sender, body, receivedAt) ?: return IngestResult.Unrecognized
        return if (writer.saveIfNew(parsed, receivedAt)) {
            IngestResult.Saved(parsed.institutionName)
        } else {
            IngestResult.Duplicate(parsed.institutionName)
        }
    }
}
