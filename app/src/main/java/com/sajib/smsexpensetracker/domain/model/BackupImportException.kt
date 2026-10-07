package com.sajib.smsexpensetracker.domain.model

/**
 * Thrown when a backup file cannot be imported.
 *
 * The message holds only the [error] category and, when known, a [line] and
 * [column]. It never holds message content, and no cause is attached: XML
 * parsers put attribute values, which are SMS bodies, into their own
 * exception messages (AGENT.md Hard Constraint 1, rule 12).
 */
class BackupImportException(
    val error: BackupImportError,
    val line: Int? = null,
    val column: Int? = null
) : Exception(describe(error, line, column)) {

    private companion object {
        fun describe(error: BackupImportError, line: Int?, column: Int?): String = buildString {
            append(error.name)
            if (line != null) append(" at line ").append(line)
            if (column != null) append(", column ").append(column)
        }
    }
}
