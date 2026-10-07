package com.sajib.smsexpensetracker.data.backup

import android.net.Uri
import java.io.InputStream

/**
 * Opens a backup file the user picked with the system file picker. Kept as
 * an interface so callers never touch ContentResolver directly.
 */
interface BackupFileOpener {

    /**
     * Opens [uri] for one sequential read, passes the stream to [block] and
     * closes it afterwards, also when [block] fails or is cancelled.
     *
     * @throws com.sajib.smsexpensetracker.domain.model.BackupImportException
     * with READ_FAILED when the file cannot be opened.
     */
    suspend fun <T> read(uri: Uri, block: suspend (InputStream) -> T): T
}
