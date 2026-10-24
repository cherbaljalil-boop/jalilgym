package com.chdjalil.jalilgym.core.domain.repository

import kotlinx.coroutines.flow.Flow

enum class CloudSyncError { NETWORK, INVALID_EMAIL, INVALID_CODE, RATE_LIMITED, SESSION_EXPIRED, UNKNOWN }

data class CloudSyncStatus(
    /** False when this build has no Supabase project configured; cloud sync is hidden then. */
    val available: Boolean = false,
    /** The signed-in account; null when signed out. */
    val email: String? = null,
    /** Set between "send code" and entering it. */
    val codeSentTo: String? = null,
    val syncing: Boolean = false,
    /** Epoch millis of the last successful sync. */
    val lastSyncedAt: Long? = null,
    /** Changes on this phone not uploaded yet. */
    val pendingChanges: Int = 0,
    /** What went wrong last, until the next attempt succeeds. */
    val error: CloudSyncError? = null,
) {
    val signedIn: Boolean get() = email != null
}

/**
 * Optional account and sync through Supabase (docs/supabase/SETUP.md). Sign-in
 * is by a code sent to the user's email. Everything works without it.
 */
interface CloudSyncRepository {
    fun observeStatus(): Flow<CloudSyncStatus>

    /** Call once when the app starts, before the first [sync]. */
    suspend fun initialize()

    /** Emails a sign-in code; creates the account the first time. */
    suspend fun sendCode(email: String): Boolean

    suspend fun verifyCode(code: String): Boolean

    /** Back to entering an email. */
    suspend fun cancelSignIn()

    /** Pulls changes from other devices, then uploads this one's. No-op when signed out. */
    suspend fun sync(): Boolean

    /** Signs out on this phone; data on the phone and in the account stays. */
    suspend fun signOut()

    /** Deletes the account and everything stored in it; data on the phone stays. */
    suspend fun deleteAccount(): Boolean
}
