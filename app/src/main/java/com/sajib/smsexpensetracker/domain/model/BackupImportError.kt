package com.sajib.smsexpensetracker.domain.model

/** Why a backup import stopped. */
enum class BackupImportError {

    /** The file is not an SMS Backup & Restore XML file (wrong or missing root element, DOCTYPE, plain text). */
    NOT_SMS_BACKUP,

    /** The file starts as a backup but is damaged or truncated part way through. */
    MALFORMED,

    /** The file could not be opened or read. */
    READ_FAILED
}
