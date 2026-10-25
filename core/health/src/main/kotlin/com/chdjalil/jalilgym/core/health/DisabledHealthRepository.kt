package com.chdjalil.jalilgym.core.health

import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthStatus
import com.chdjalil.jalilgym.core.domain.repository.HealthSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Health Connect left out of the build: never asks for anything and never syncs. */
internal object DisabledHealthRepository : HealthRepository {
    override val requiredPermissions: Set<String> = emptySet()

    override fun observeStatus(): Flow<HealthStatus> = flowOf(HealthStatus(availability = HealthAvailability.OFF_IN_THIS_BUILD))

    override suspend fun refresh() = Unit

    override suspend fun setEnabled(enabled: Boolean) = Unit

    override suspend fun sync(): HealthSyncResult? = null

    override fun observeStepsToday(): Flow<Long?> = flowOf(null)

    override suspend fun refreshSteps() = Unit
}
