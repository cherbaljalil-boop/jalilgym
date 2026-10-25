package com.chdjalil.jalilgym.core.domain.repository

import kotlinx.coroutines.flow.Flow

enum class HealthAvailability {
    /** The device can't run Health Connect. */
    NOT_SUPPORTED,

    /** Health Connect needs installing or updating from Play. */
    NEEDS_INSTALL_OR_UPDATE,
    AVAILABLE,

    /** Left out of this build (see core/health/build.gradle.kts); nothing about it is shown. */
    OFF_IN_THIS_BUILD,
}

data class HealthStatus(
    val availability: HealthAvailability = HealthAvailability.NOT_SUPPORTED,
    /** The user turned the integration on in Profile. */
    val enabled: Boolean = false,
    val granted: Set<String> = emptySet(),
    val required: Set<String> = emptySet(),
    /** When data was last written, epoch millis. */
    val lastSyncedAt: Long? = null,
) {
    val ready: Boolean get() = availability == HealthAvailability.AVAILABLE && enabled && granted.containsAll(required)
}

/** What a sync wrote, removed and brought in. */
data class HealthSyncResult(val written: Int, val deleted: Int, val importedWeights: Int)

/**
 * Optional Health Connect integration (docs/BLUEPRINT.md §5.5): writes
 * workouts, body weight and nutrition, reads weight from other apps and
 * today's steps. Everything is a no-op until [HealthStatus.ready].
 */
interface HealthRepository {
    /** Permission strings to request with Health Connect's permission contract. */
    val requiredPermissions: Set<String>

    fun observeStatus(): Flow<HealthStatus>

    /** Re-checks availability and granted permissions (e.g. after the permission screen). */
    suspend fun refresh()

    suspend fun setEnabled(enabled: Boolean)

    /** Brings Health Connect in line with the last 30 days here. Null when not ready. */
    suspend fun sync(): HealthSyncResult?

    /** Today's steps from Health Connect; null when not ready or unknown. */
    fun observeStepsToday(): Flow<Long?>

    suspend fun refreshSteps()
}
