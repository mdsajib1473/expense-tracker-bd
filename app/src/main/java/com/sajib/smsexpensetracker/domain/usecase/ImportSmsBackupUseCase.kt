package com.sajib.smsexpensetracker.domain.usecase

import com.sajib.smsexpensetracker.data.backup.SmsBackupEntry
import com.sajib.smsexpensetracker.data.backup.SmsBackupReader
import com.sajib.smsexpensetracker.domain.model.ImportProgress
import com.sajib.smsexpensetracker.domain.model.ImportSummary
import com.sajib.smsexpensetracker.domain.model.IngestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * Imports an SMS Backup & Restore XML export: every received SMS in it goes
 * through the single ingestion entry point ([IngestSmsUseCase]), exactly like
 * a live or inbox message. Sent and other non-inbox messages are counted and
 * never parsed; incomplete ones are counted as invalid.
 *
 * Rows saved before a failure or a cancellation stay saved. That is safe:
 * re-importing the same file is idempotent through the duplicate check.
 *
 * Does no logging. Its only output is an [ImportSummary] of counts, so no
 * body, sender or institution list can leak from here (AGENT.md rule 12).
 */
class ImportSmsBackupUseCase @Inject constructor(
    private val reader: SmsBackupReader,
    private val ingestSms: IngestSmsUseCase
) {

    /**
     * Reads [input] to the end on the IO dispatcher; the caller opens and
     * closes it. Reports [onProgress] at the start, every [PROGRESS_STEP]
     * messages and at the end, on the IO dispatcher. Cancellable between
     * messages.
     *
     * @throws com.sajib.smsexpensetracker.domain.model.BackupImportException
     * when the file is not a backup, is damaged or cannot be read.
     */
    suspend operator fun invoke(
        input: InputStream,
        onProgress: (ImportProgress) -> Unit = {}
    ): ImportSummary = withContext(Dispatchers.IO) {
        val document = reader.open(input)
        var processed = 0
        var saved = 0
        var duplicates = 0
        var unrecognized = 0
        var skippedNotInbox = 0
        var invalid = 0

        onProgress(ImportProgress(processed, document.countHint))
        for (entry in document.entries) {
            ensureActive()
            when (entry) {
                is SmsBackupEntry.Inbox -> {
                    val message = entry.message
                    when (ingestSms(message.sender, message.body, message.receivedAtMillis)) {
                        is IngestResult.Saved -> saved++
                        is IngestResult.Duplicate -> duplicates++
                        IngestResult.Unrecognized -> unrecognized++
                    }
                }
                SmsBackupEntry.NotInbox -> skippedNotInbox++
                SmsBackupEntry.Invalid -> invalid++
            }
            processed++
            if (processed % PROGRESS_STEP == 0) onProgress(ImportProgress(processed, document.countHint))
        }
        onProgress(ImportProgress(processed, document.countHint))

        ImportSummary(
            totalRead = processed,
            saved = saved,
            duplicates = duplicates,
            unrecognized = unrecognized,
            skippedNotInbox = skippedNotInbox,
            invalid = invalid
        )
    }

    private companion object {
        const val PROGRESS_STEP = 25
    }
}
