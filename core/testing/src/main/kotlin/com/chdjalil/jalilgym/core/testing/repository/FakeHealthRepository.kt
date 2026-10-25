package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthStatus
import com.chdjalil.jalilgym.core.domain.repository.HealthSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeHealthRepository(
    availability: HealthAvailability = HealthAvailability.AVAILABLE,
) : HealthRepository {
    override val requiredPermissions: Set<String> = setOf("read-steps", "write-weight")

    val status = MutableStateFlow(HealthStatus(availability = availability, required = requiredPermissions))
    val steps = MutableStateFlow<Long?>(null)
    var syncs = 0

    /** What the next [refresh] reports as granted. */
    var grantOnRefresh: Set<String> = emptySet()

    override fun observeStatus(): Flow<HealthStatus> = status

    override suspend fun refresh() = status.update { it.copy(granted = grantOnRefresh) }

    override suspend fun setEnabled(enabled: Boolean) = status.update { it.copy(enabled = enabled) }

    override suspend fun sync(): HealthSyncResult? {
        if (!status.value.ready) return null
        syncs++
        return HealthSyncResult(0, 0, 0)
    }

    override fun observeStepsToday(): Flow<Long?> = steps

    override suspend fun refreshSteps() = Unit
}
