package com.chdjalil.jalilgym.core.domain.repository

/** What an import did, or why it didn't. */
sealed interface ImportResult {
    /** [rows] database rows restored. */
    data class Success(val rows: Int, val exportedAt: Long) : ImportResult

    /** Made by a newer version of the app; update first. */
    data object TooNew : ImportResult

    /** Not a Jalilgym backup, or damaged. */
    data object Invalid : ImportResult
}

/**
 * Manual export and import of everything except progress photos, as one JSON
 * file the user picks (docs/BLUEPRINT.md §5.7). Locations are content URIs.
 */
interface BackupRepository {
    /** Writes the backup and returns how many rows it holds. */
    suspend fun export(destinationUri: String): Int

    /** Replaces all data (photos excepted) with the backup's. */
    suspend fun import(sourceUri: String): ImportResult
}
