package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.BackupRepository
import com.chdjalil.jalilgym.core.domain.repository.ImportResult

class FakeBackupRepository(
    var exportRows: Int = 42,
    var importResult: ImportResult = ImportResult.Success(rows = 42, exportedAt = 0L),
) : BackupRepository {
    val exportedTo = mutableListOf<String>()
    val importedFrom = mutableListOf<String>()

    /** When set, [export] throws it (e.g. the user revoked storage access). */
    var exportFailure: Exception? = null

    override suspend fun export(destinationUri: String): Int {
        exportFailure?.let { throw it }
        exportedTo += destinationUri
        return exportRows
    }

    override suspend fun import(sourceUri: String): ImportResult {
        importedFrom += sourceUri
        return importResult
    }
}
