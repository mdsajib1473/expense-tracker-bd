package com.sajib.smsexpensetracker.data.backup

import android.content.Context
import android.net.Uri
import com.sajib.smsexpensetracker.domain.model.BackupImportError
import com.sajib.smsexpensetracker.domain.model.BackupImportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject

/**
 * Reads the picked document through ContentResolver using the temporary read
 * grant that comes with the picker result. The file is never copied, and no
 * persistable URI permission is taken, so the app keeps no access to it
 * afterwards. Needs no storage permission.
 */
class ContentResolverBackupFileOpener @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupFileOpener {

    /** Opens, runs [block] and closes all on the IO dispatcher, so no opened stream can escape a cancellation. */
    override suspend fun <T> read(uri: Uri, block: suspend (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val stream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            throw BackupImportException(BackupImportError.READ_FAILED)
        } catch (e: SecurityException) {
            throw BackupImportException(BackupImportError.READ_FAILED)
        } ?: throw BackupImportException(BackupImportError.READ_FAILED)
        stream.use { block(it) }
    }
}
