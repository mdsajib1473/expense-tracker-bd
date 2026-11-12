package com.sajib.smsexpensetracker.domain.model

/** Size limits applied to message text before any parser sees it. */
object SmsLimits {

    /**
     * Longest message body, in UTF-16 characters, that is handed to the
     * parsers. A longer body is treated as unrecognized without being
     * parsed: SMS text is untrusted input, and parser patterns can take
     * quadratic time on a very long crafted body. Real bank and wallet
     * messages are a few hundred characters at most.
     *
     * Enforced by IngestSmsUseCase for the live receiver, the inbox import
     * and the backup file import. The share and paste intake has its own,
     * stricter limit (PrepareManualEntryUseCase.MAX_TEXT_LENGTH), which must
     * never exceed this one.
     */
    const val MAX_BODY_LENGTH = 5000
}
