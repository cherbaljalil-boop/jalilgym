package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.CloudSyncError
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncRepository
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCloudSyncRepository(available: Boolean = true) : CloudSyncRepository {
    val status = MutableStateFlow(CloudSyncStatus(available = available))

    /** The code [verifyCode] accepts. */
    var validCode = "123456"
    var syncs = 0

    override fun observeStatus(): Flow<CloudSyncStatus> = status

    override suspend fun sendCode(email: String): Boolean {
        if ("@" !in email) {
            status.update { it.copy(error = CloudSyncError.INVALID_EMAIL) }
            return false
        }
        status.update { it.copy(codeSentTo = email.trim(), error = null) }
        return true
    }

    override suspend fun verifyCode(code: String): Boolean {
        val email = status.value.codeSentTo ?: return false
        if (code != validCode) {
            status.update { it.copy(error = CloudSyncError.INVALID_CODE) }
            return false
        }
        status.update { it.copy(email = email, codeSentTo = null, error = null) }
        return true
    }

    override suspend fun cancelSignIn() = status.update { it.copy(codeSentTo = null, error = null) }

    override suspend fun sync(): Boolean {
        if (!status.value.signedIn) return false
        syncs++
        status.update { it.copy(lastSyncedAt = 1L, pendingChanges = 0) }
        return true
    }

    override suspend fun signOut() = status.update { CloudSyncStatus(available = it.available) }

    override suspend fun deleteAccount(): Boolean {
        signOut()
        return true
    }
}
