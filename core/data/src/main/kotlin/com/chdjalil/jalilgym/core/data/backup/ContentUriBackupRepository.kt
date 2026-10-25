package com.chdjalil.jalilgym.core.data.backup

import android.content.Context
import android.net.Uri
import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import com.chdjalil.jalilgym.core.domain.repository.BackupRepository
import com.chdjalil.jalilgym.core.domain.repository.ImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads and writes backups at Storage Access Framework URIs the user picked. */
internal class ContentUriBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serializer: BackupSerializer,
    @Dispatcher(JgDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun export(destinationUri: String): Int = withContext(ioDispatcher) {
        // "wt" truncates when overwriting an existing file.
        val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri), "wt")
            ?: throw IOException("Cannot write $destinationUri")
        output.use { serializer.write(it) }
    }

    override suspend fun import(sourceUri: String): ImportResult = withContext(ioDispatcher) {
        val input = context.contentResolver.openInputStream(Uri.parse(sourceUri)) ?: return@withContext ImportResult.Invalid
        input.use { serializer.read(it) }
    }
}
