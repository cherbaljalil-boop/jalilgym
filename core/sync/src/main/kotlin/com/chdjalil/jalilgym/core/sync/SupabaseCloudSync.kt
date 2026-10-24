package com.chdjalil.jalilgym.core.sync

import android.content.Context
import android.util.Log
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncError
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncRepository
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Where the sign-in lives on this phone; left out of Android backups (data_extraction_rules.xml). */
interface SessionStore {
    var session: SupabaseSession?
    var codeSentTo: String?
    var lastSyncedAt: Long?
}

internal class SharedPreferencesSessionStore @Inject constructor(@ApplicationContext context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override var session: SupabaseSession?
        get() = prefs.getString(SESSION, null)?.let { runCatching { json.decodeFromString(SupabaseSession.serializer(), it) }.getOrNull() }
        set(value) = prefs.edit().putString(SESSION, value?.let { json.encodeToString(SupabaseSession.serializer(), it) }).apply()

    override var codeSentTo: String?
        get() = prefs.getString(CODE_SENT_TO, null)
        set(value) = prefs.edit().putString(CODE_SENT_TO, value).apply()

    override var lastSyncedAt: Long?
        get() = prefs.getLong(LAST_SYNC, 0).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(LAST_SYNC, value ?: 0).apply()

    private companion object {
        const val FILE = "jalilgym_cloud"
        const val SESSION = "session"
        const val CODE_SENT_TO = "code_sent_to"
        const val LAST_SYNC = "last_synced_at"
    }
}

@Singleton
class SupabaseCloudSync @Inject constructor(
    private val config: SupabaseConfig,
    private val client: SupabaseClient,
    private val engine: SyncEngine,
    private val store: SessionStore,
) : CloudSyncRepository {

    private val mutex = Mutex()
    private val status = MutableStateFlow(
        CloudSyncStatus(
            available = config.isConfigured,
            email = store.session?.email,
            codeSentTo = store.codeSentTo,
            lastSyncedAt = store.lastSyncedAt,
        ),
    )

    override fun observeStatus(): Flow<CloudSyncStatus> = status.asStateFlow()

    /**
     * Call once at start. Turns tracking off when there's no sign-in (for example
     * a database restored from an Android backup without its session) and adds
     * tracking to tables created by an app update.
     */
    override suspend fun initialize() {
        if (!config.isConfigured || store.session == null) {
            engine.disableTracking()
        } else {
            engine.enableTracking(markEverything = false)
            status.update { it.copy(pendingChanges = engine.pendingChanges()) }
        }
    }

    override suspend fun sendCode(email: String): Boolean = attempt(signInStep = true) {
        val address = email.trim()
        if (!EMAIL.matches(address)) throw SupabaseException(400, "Bad email")
        client.sendCode(address)
        store.codeSentTo = address
        status.update { it.copy(codeSentTo = address) }
    }

    override suspend fun verifyCode(code: String): Boolean {
        val email = store.codeSentTo ?: return false
        val signedIn = attempt(signInStep = true, invalidCode = true) {
            val session = client.verifyCode(email, code.filter(Char::isDigit))
            store.session = session
            store.codeSentTo = null
            // Everything already on the phone goes up with the first sync.
            engine.enableTracking(markEverything = true)
            status.update { it.copy(email = session.email, codeSentTo = null) }
        }
        return signedIn && sync()
    }

    override suspend fun cancelSignIn() {
        store.codeSentTo = null
        status.update { it.copy(codeSentTo = null, error = null) }
    }

    override suspend fun sync(): Boolean = mutex.withLock {
        if (!config.isConfigured || store.session == null) return false
        status.update { it.copy(syncing = true) }
        val ok = attempt {
            val session = validSession()
            engine.enableTracking(markEverything = false)
            engine.sync(
                object : SyncRemote {
                    override suspend fun push(rows: List<SyncRow>) = client.push(session, rows)

                    override suspend fun pull(afterSeq: Long, limit: Int) = client.pull(session, afterSeq, limit)
                },
            )
            val now = System.currentTimeMillis()
            store.lastSyncedAt = now
            status.update { it.copy(lastSyncedAt = now) }
        }
        status.update { it.copy(syncing = false, pendingChanges = runCatching { engine.pendingChanges() }.getOrDefault(it.pendingChanges)) }
        ok
    }

    override suspend fun signOut() {
        store.session?.let { session -> runCatching { client.signOut(session) } }
        forgetAccount()
    }

    override suspend fun deleteAccount(): Boolean {
        val ok = attempt { client.deleteAccount(validSession()) }
        if (ok) forgetAccount()
        return ok
    }

    private suspend fun forgetAccount() {
        engine.disableTracking()
        store.session = null
        store.codeSentTo = null
        store.lastSyncedAt = null
        status.value = CloudSyncStatus(available = config.isConfigured)
    }

    /** The stored session, refreshed when it's within a minute of expiring. */
    private suspend fun validSession(): SupabaseSession {
        val session = store.session ?: throw SupabaseException(401, "Signed out")
        if (session.expiresAt - REFRESH_MARGIN_SECONDS > System.currentTimeMillis() / 1_000) return session
        return try {
            client.refresh(session).also { store.session = it }
        } catch (e: SupabaseException) {
            if (e.status in 400..499) {
                forgetAccount()
                status.update { it.copy(error = CloudSyncError.SESSION_EXPIRED) }
            }
            throw e
        }
    }

    private suspend fun attempt(signInStep: Boolean = false, invalidCode: Boolean = false, block: suspend () -> Unit): Boolean = try {
        block()
        status.update { it.copy(error = null) }
        true
    } catch (e: SupabaseException) {
        Log.w(TAG, "Cloud sync request failed (${e.status})")
        val error = when {
            e.status == 0 -> CloudSyncError.NETWORK
            e.status == 429 -> CloudSyncError.RATE_LIMITED
            invalidCode && e.status in 400..499 -> CloudSyncError.INVALID_CODE
            signInStep && e.status in 400..499 -> CloudSyncError.INVALID_EMAIL
            e.status == 401 -> CloudSyncError.SESSION_EXPIRED
            else -> CloudSyncError.UNKNOWN
        }
        status.update { if (it.error == CloudSyncError.SESSION_EXPIRED) it else it.copy(error = error) }
        false
    }

    private companion object {
        const val TAG = "CloudSync"
        const val REFRESH_MARGIN_SECONDS = 60
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
