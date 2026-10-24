package com.chdjalil.jalilgym.core.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** One changed row as stored on the server; [data] is null for a deletion. */
data class SyncRow(
    val table: String,
    val key: String,
    val data: JsonObject?,
    /** When the row changed on the device that changed it, epoch millis. */
    val updatedAt: Long,
    /** Server order, set by the server. */
    val seq: Long = 0,
)

/** Where rows go and come from: Supabase in the app, a fake in tests. */
interface SyncRemote {
    suspend fun push(rows: List<SyncRow>)

    /** Rows with a seq above [afterSeq], in seq order. */
    suspend fun pull(afterSeq: Long, limit: Int): List<SyncRow>
}

data class SyncReport(val pulled: Int, val pushed: Int)

internal const val CHANGES_TABLE = "sync_changes"
internal const val STATE_TABLE = "sync_state"

/**
 * Not synced: SQLite and Room internals, the sync bookkeeping itself, and
 * progress photos (their image files stay on the phone, as in backups).
 */
val LOCAL_ONLY_TABLES = setOf("android_metadata", "room_master_table", "sqlite_sequence", "progress_photos", CHANGES_TABLE, STATE_TABLE)

private const val KEY_SEPARATOR = '\u001f'
private const val BLOB_KEY = "base64"
private const val PUSH_BATCH = 500
private const val PULL_PAGE = 1_000

/** Epoch millis inside SQLite. */
private const val SQL_NOW = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

private class TableInfo(val name: String, val columns: List<String>, val primaryKey: List<String>, val parents: Set<String>)

/**
 * Row-level sync between the local database and a [SyncRemote].
 *
 * Triggers on every synced table record what changed in [CHANGES_TABLE]
 * (table, primary key, when). A sync first pulls rows changed elsewhere and
 * applies them — skipping any row changed here more recently — with the
 * triggers muted so nothing echoes back, then pushes local changes. The newest
 * change to a row wins on both sides.
 */
@Singleton
class SyncEngine @Inject constructor(private val database: JalilgymDatabase) {

    private val mutex = Mutex()
    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    /** Turns tracking on. With [markEverything], every existing row is queued for upload (first sign-in). */
    suspend fun enableTracking(markEverything: Boolean) = mutex.withLock {
        database.withTransaction {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $CHANGES_TABLE (table_name TEXT NOT NULL, row_key TEXT NOT NULL, " +
                    "changed_at INTEGER NOT NULL, PRIMARY KEY (table_name, row_key))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $STATE_TABLE (id INTEGER PRIMARY KEY CHECK (id = 1), " +
                    "applying INTEGER NOT NULL DEFAULT 0, cursor INTEGER NOT NULL DEFAULT 0)",
            )
            db.execSQL("INSERT OR IGNORE INTO $STATE_TABLE (id, applying, cursor) VALUES (1, 0, 0)")
            tables().forEach { table ->
                installTriggers(table)
                if (markEverything) {
                    db.execSQL("INSERT OR REPLACE INTO $CHANGES_TABLE SELECT '${table.name}', ${keyExpression(table, null)}, $SQL_NOW FROM `${table.name}`")
                }
            }
        }
    }

    /** Turns tracking off and forgets pending changes and the pull position (sign-out). */
    suspend fun disableTracking() = mutex.withLock {
        database.withTransaction {
            tables().forEach { table -> TRIGGERS.forEach { db.execSQL("DROP TRIGGER IF EXISTS sync_${it}_${table.name}") } }
            db.execSQL("DROP TABLE IF EXISTS $CHANGES_TABLE")
            db.execSQL("DROP TABLE IF EXISTS $STATE_TABLE")
        }
    }

    suspend fun pendingChanges(): Int = mutex.withLock {
        if (!tracking()) {
            0
        } else {
            db.query("SELECT COUNT(*) FROM $CHANGES_TABLE").use {
                it.moveToFirst()
                it.getInt(0)
            }
        }
    }

    suspend fun sync(remote: SyncRemote): SyncReport = mutex.withLock {
        check(tracking()) { "Tracking is off" }
        val pulled = pull(remote)
        val pushed = push(remote)
        SyncReport(pulled, pushed)
    }

    private fun tracking(): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$STATE_TABLE'").use { it.moveToFirst() }

    private fun tables(): List<TableInfo> {
        val names = db.query("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }.filter { it !in LOCAL_ONLY_TABLES && !it.startsWith("sqlite_") && !it.startsWith("room_") }
        return names.map { name ->
            val columns = mutableListOf<String>()
            val pk = sortedMapOf<Int, String>()
            db.query("PRAGMA table_info(`$name`)").use { c ->
                while (c.moveToNext()) {
                    val column = c.getString(c.getColumnIndexOrThrow("name"))
                    columns += column
                    val position = c.getInt(c.getColumnIndexOrThrow("pk"))
                    if (position > 0) pk[position] = column
                }
            }
            val parents = db.query("PRAGMA foreign_key_list(`$name`)").use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("table"))) }
            }
            TableInfo(name, columns, pk.values.toList(), parents - name)
        }
    }

    /** SQL for a row's key: its primary key values joined by a separator. */
    private fun keyExpression(table: TableInfo, row: String?): String =
        table.primaryKey.joinToString(" || char(31) || ") { "CAST(${row?.let { "$it." } ?: ""}`$it` AS TEXT)" }

    private fun installTriggers(table: TableInfo) {
        val muted = "WHEN NOT EXISTS (SELECT 1 FROM $STATE_TABLE WHERE applying = 1)"
        fun record(row: String) =
            "BEGIN INSERT OR REPLACE INTO $CHANGES_TABLE VALUES ('${table.name}', ${keyExpression(table, row)}, $SQL_NOW); END"
        db.execSQL("CREATE TRIGGER IF NOT EXISTS sync_i_${table.name} AFTER INSERT ON `${table.name}` $muted ${record("NEW")}")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS sync_u_${table.name} AFTER UPDATE ON `${table.name}` $muted ${record("NEW")}")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS sync_d_${table.name} AFTER DELETE ON `${table.name}` $muted ${record("OLD")}")
    }

    private fun keyWhere(table: TableInfo, key: String): Pair<String, Array<Any?>> {
        val values = key.split(KEY_SEPARATOR)
        require(values.size == table.primaryKey.size) { "Bad key for ${table.name}" }
        return table.primaryKey.joinToString(" AND ") { "`$it` = ?" } to values.toTypedArray()
    }

    private suspend fun push(remote: SyncRemote): Int {
        var pushed = 0
        val byName = tables().associateBy { it.name }
        while (true) {
            val pending = db.query("SELECT table_name, row_key, changed_at FROM $CHANGES_TABLE ORDER BY changed_at LIMIT $PUSH_BATCH").use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getLong(2))) }
            }
            if (pending.isEmpty()) return pushed
            val rows = pending.mapNotNull { (name, key, changedAt) ->
                val table = byName[name] ?: return@mapNotNull null
                SyncRow(name, key, readRow(table, key), changedAt)
            }
            if (rows.isNotEmpty()) remote.push(rows)
            database.withTransaction {
                // A row changed again while uploading keeps its newer entry.
                pending.forEach { (name, key, changedAt) ->
                    db.execSQL("DELETE FROM $CHANGES_TABLE WHERE table_name = ? AND row_key = ? AND changed_at = ?", arrayOf(name, key, changedAt))
                }
            }
            pushed += rows.size
        }
    }

    private fun readRow(table: TableInfo, key: String): JsonObject? {
        val (where, args) = keyWhere(table, key)
        return db.query("SELECT * FROM `${table.name}` WHERE $where", args).use { c ->
            if (!c.moveToFirst()) null else JsonObject((0 until c.columnCount).associate { c.getColumnName(it) to c.value(it) })
        }
    }

    private suspend fun pull(remote: SyncRemote): Int {
        var cursor = db.query("SELECT cursor FROM $STATE_TABLE WHERE id = 1").use {
            it.moveToFirst()
            it.getLong(0)
        }
        val rows = mutableListOf<SyncRow>()
        while (true) {
            val page = remote.pull(cursor, PULL_PAGE)
            rows += page
            if (page.isNotEmpty()) cursor = page.maxOf { it.seq }
            if (page.size < PULL_PAGE) break
        }
        if (rows.isEmpty()) return 0
        val newest = rows.groupBy { it.table to it.key }.values.map { same -> same.maxBy { it.seq } }
        database.withTransaction {
            db.execSQL("UPDATE $STATE_TABLE SET applying = 1 WHERE id = 1")
            db.query("PRAGMA defer_foreign_keys = ON").use { it.moveToFirst() }
            apply(newest)
            removeOrphans()
            db.execSQL("UPDATE $STATE_TABLE SET applying = 0, cursor = ? WHERE id = 1", arrayOf(cursor))
        }
        return newest.size
    }

    private fun apply(rows: List<SyncRow>) {
        val tables = tables()
        val byName = tables.associateBy { it.name }
        val depth = depths(tables)
        val (deletions, upserts) = rows.filter { it.table in byName }.partition { it.data == null }
        // Parents before children when writing, children before parents when deleting.
        upserts.sortedBy { depth[it.table] }.forEach { row ->
            val table = byName.getValue(row.table)
            if (localIsNewer(row)) return@forEach
            val values = ContentValues()
            row.data!!.forEach { (column, value) -> if (column in table.columns) values.putJson(column, value) }
            val (where, args) = keyWhere(table, row.key)
            val updated = db.update(table.name, SQLiteDatabase.CONFLICT_ABORT, values, where, args)
            if (updated == 0) db.insert(table.name, SQLiteDatabase.CONFLICT_IGNORE, values)
        }
        deletions.sortedByDescending { depth[it.table] }.forEach { row ->
            val table = byName.getValue(row.table)
            if (localIsNewer(row)) return@forEach
            val (where, args) = keyWhere(table, row.key)
            try {
                db.delete(table.name, where, args)
            } catch (_: SQLiteConstraintException) {
                // Still referenced by a row this device kept (RESTRICT); keep it rather than fail the sync.
            }
        }
    }

    /** True when this device changed the row after [row] was made; otherwise the local change is superseded. */
    private fun localIsNewer(row: SyncRow): Boolean {
        val local = db.query("SELECT changed_at FROM $CHANGES_TABLE WHERE table_name = ? AND row_key = ?", arrayOf(row.table, row.key)).use {
            if (it.moveToFirst()) it.getLong(0) else null
        } ?: return false
        if (local > row.updatedAt) return true
        db.execSQL("DELETE FROM $CHANGES_TABLE WHERE table_name = ? AND row_key = ?", arrayOf(row.table, row.key))
        return false
    }

    /** Rows whose parent never arrived (or was deleted) are dropped so foreign keys hold. */
    private fun removeOrphans() {
        repeat(10) {
            val orphans = db.query("PRAGMA foreign_key_check").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) }
            }
            if (orphans.isEmpty()) return
            orphans.forEach { (table, rowId) -> db.execSQL("DELETE FROM `$table` WHERE rowid = ?", arrayOf(rowId)) }
        }
    }

    private fun depths(tables: List<TableInfo>): Map<String, Int> {
        val byName = tables.associateBy { it.name }
        val memo = mutableMapOf<String, Int>()
        fun depth(name: String, seen: Set<String>): Int = memo[name] ?: run {
            val parents = byName[name]?.parents.orEmpty().filter { it in byName && it !in seen }
            (parents.maxOfOrNull { depth(it, seen + name) + 1 } ?: 0).also { memo[name] = it }
        }
        return tables.associate { it.name to depth(it.name, emptySet()) }
    }

    private fun Cursor.value(index: Int): JsonElement = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JsonNull
        Cursor.FIELD_TYPE_INTEGER -> JsonPrimitive(getLong(index))
        Cursor.FIELD_TYPE_FLOAT -> JsonPrimitive(getDouble(index))
        Cursor.FIELD_TYPE_BLOB -> buildJsonObject { put(BLOB_KEY, JsonPrimitive(Base64.encodeToString(getBlob(index), Base64.NO_WRAP))) }
        else -> JsonPrimitive(getString(index))
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

    private companion object {
        val TRIGGERS = listOf("i", "u", "d")
    }
}
