package com.chdjalil.jalilgym.core.health

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Remembers which client record IDs were written last time, so rows deleted
 * here can be deleted from Health Connect too.
 */
interface SyncLedger {
    fun written(kind: String): Set<String>

    fun setWritten(kind: String, ids: Set<String>)

    var lastSyncedAt: Long?
}

internal class SharedPreferencesSyncLedger @Inject constructor(@ApplicationContext context: Context) : SyncLedger {
    private val prefs = context.getSharedPreferences("health_connect_sync", Context.MODE_PRIVATE)

    override fun written(kind: String): Set<String> = prefs.getStringSet(kind, emptySet()).orEmpty().toSet()

    override fun setWritten(kind: String, ids: Set<String>) = prefs.edit().putStringSet(kind, ids).apply()

    override var lastSyncedAt: Long?
        get() = prefs.getLong(LAST_SYNC, 0).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(LAST_SYNC, value ?: 0).apply()

    private companion object {
        const val LAST_SYNC = "lastSyncedAt"
    }
}
