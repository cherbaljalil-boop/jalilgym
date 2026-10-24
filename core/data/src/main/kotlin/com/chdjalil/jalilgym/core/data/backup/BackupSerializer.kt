package com.chdjalil.jalilgym.core.data.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.domain.repository.ImportResult
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal const val BACKUP_FORMAT = "jalilgym-backup"

/** Bump when the file layout (not the database schema) changes. */
internal const val BACKUP_VERSION = 1

/**
 * Tables left out: SQLite and Room internals, and progress photos (the image
 * files aren't in the backup, so their rows would point at nothing).
 */
private val SKIPPED_TABLES = setOf(
    "android_metadata", "room_master_table", "sqlite_sequence", "progress_photos",
    // Cloud sync bookkeeping (:core:sync) belongs to this phone and account.
    "sync_changes", "sync_state",
)

private const val BLOB_KEY = "base64"

/** Rows in the backup reference rows that aren't there. */
private class BrokenBackupException : RuntimeException("Backup has dangling references")

/**
 * Writes and reads the backup file: `{format, backupVersion, schemaVersion,
 * exportedAt, preferences, tables: {name: [{column: value}]}}`. Rows are copied
 * column by column, so the file follows the schema without per-entity code.
 * Older backups import into newer schemas as long as new columns are nullable
 * or have defaults (true of every auto-migration so far).
 */
class BackupSerializer @Inject constructor(
    private val database: JalilgymDatabase,
    private val preferences: JgPreferencesDataSource,
) {
    private val json = Json { prettyPrint = false }

    private fun SupportSQLiteDatabase.tables(): List<String> =
        query("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }.filter { it !in SKIPPED_TABLES && !it.startsWith("sqlite_") && !it.startsWith("room_") }

    private fun SupportSQLiteDatabase.columns(table: String): Set<String> =
        query("PRAGMA table_info(`$table`)").use { c ->
            val name = c.getColumnIndexOrThrow("name")
            buildSet { while (c.moveToNext()) add(c.getString(name)) }
        }

    private fun Cursor.value(index: Int): JsonElement = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JsonNull
        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(index))
        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(index))
        Cursor.FIELD_TYPE_BLOB -> buildJsonObject { put(BLOB_KEY, Base64.encodeToString(getBlob(index), Base64.NO_WRAP)) }
        else -> JsonPrimitive(getString(index))
    }

    /** Writes the backup and returns the number of rows. */
    suspend fun write(output: OutputStream, now: Long = System.currentTimeMillis()): Int {
        var rows = 0
        val tables = database.withTransaction {
            val db = database.openHelper.writableDatabase
            JsonObject(
                db.tables().associateWith { table ->
                    db.query("SELECT * FROM `$table`").use { c ->
                        JsonArray(
                            buildList {
                                while (c.moveToNext()) {
                                    add(JsonObject((0 until c.columnCount).associate { c.getColumnName(it) to c.value(it) }))
                                }
                            },
                        ).also { rows += it.size }
                    }
                },
            )
        }
        val root = buildJsonObject {
            put("format", BACKUP_FORMAT)
            put("backupVersion", BACKUP_VERSION)
            put("schemaVersion", database.openHelper.readableDatabase.version)
            put("exportedAt", now)
            put("preferences", preferences.exportJson())
            put("tables", tables)
        }
        output.bufferedWriter().use { it.write(json.encodeToString(JsonElement.serializer(), root)) }
        return rows
    }

    private fun ContentValues.putJson(key: String, value: JsonElement) {
        when {
            value is JsonNull -> putNull(key)
            value is JsonObject -> put(key, Base64.decode(value.getValue(BLOB_KEY).jsonPrimitive.content, Base64.NO_WRAP))
            value is JsonPrimitive && value.isString -> put(key, value.content)
            value is JsonPrimitive -> value.longOrNull?.let { put(key, it) } ?: put(key, value.doubleOrNull ?: error("Bad number $value"))
            else -> error("Unexpected value for $key")
        }
    }

    /** Replaces every table (photos excepted) and the settings with the backup's. */
    suspend fun read(input: InputStream): ImportResult {
        val root = try {
            json.parseToJsonElement(input.bufferedReader().use { it.readText() }).jsonObject
        } catch (e: SerializationException) {
            return ImportResult.Invalid
        } catch (e: IllegalArgumentException) {
            return ImportResult.Invalid
        }
        val header = runCatching {
            Triple(
                root.getValue("format").jsonPrimitive.content,
                root.getValue("backupVersion").jsonPrimitive.int,
                root.getValue("schemaVersion").jsonPrimitive.int,
            )
        }.getOrNull() ?: return ImportResult.Invalid
        val (format, backupVersion, schemaVersion) = header
        if (format != BACKUP_FORMAT) return ImportResult.Invalid
        val currentSchema = database.openHelper.readableDatabase.version
        if (backupVersion > BACKUP_VERSION || schemaVersion > currentSchema) return ImportResult.TooNew
        val tables = root["tables"] as? JsonObject ?: return ImportResult.Invalid
        val exportedAt = (root["exportedAt"] as? JsonPrimitive)?.long ?: 0L

        val rows = try {
            database.withTransaction {
                val db = database.openHelper.writableDatabase
                // Rows reference each other; foreign keys are checked once everything is in.
                db.execSQL("PRAGMA defer_foreign_keys = ON")
                val current = db.tables()
                current.forEach { db.execSQL("DELETE FROM `$it`") }
                var count = 0
                tables.forEach { (table, value) ->
                    if (table !in current) return@forEach
                    val columns = db.columns(table)
                    (value as JsonArray).forEach { row ->
                        val values = ContentValues()
                        row.jsonObject.forEach { (column, cell) -> if (column in columns) values.putJson(column, cell) }
                        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
                        count++
                    }
                }
                // Check here rather than at COMMIT: a failed COMMIT leaves SQLite's transaction open.
                db.query("PRAGMA foreign_key_check").use { if (it.moveToFirst()) throw BrokenBackupException() }
                count
            }
        } catch (e: RuntimeException) {
            // Constraint failures (and anything malformed) roll the whole import back.
            return ImportResult.Invalid
        }
        (root["preferences"] as? JsonObject)?.let { runCatching { preferences.restoreJson(it) } }
        return ImportResult.Success(rows, exportedAt)
    }
}
