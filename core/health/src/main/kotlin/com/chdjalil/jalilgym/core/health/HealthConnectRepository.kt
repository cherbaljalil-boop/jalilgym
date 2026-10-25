package com.chdjalil.jalilgym.core.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthStatus
import com.chdjalil.jalilgym.core.domain.repository.HealthSyncResult
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.MealType
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Days kept in step with Health Connect. */
internal const val SYNC_WINDOW_DAYS = 30L

private data class Probe(val availability: HealthAvailability, val granted: Set<String>)

@Singleton
internal class HealthConnectRepository @Inject constructor(
    private val gateway: HealthConnectGateway,
    private val ledger: SyncLedger,
    private val clock: Clock,
    private val preferences: UserPreferencesRepository,
    private val workouts: WorkoutRepository,
    private val body: BodyRepository,
    private val nutrition: NutritionRepository,
) : HealthRepository {

    override val requiredPermissions: Set<String> = HEALTH_PERMISSIONS

    private val probe = MutableStateFlow(Probe(HealthAvailability.NOT_SUPPORTED, emptySet()))
    private val lastSynced = MutableStateFlow(ledger.lastSyncedAt)
    private val steps = MutableStateFlow<Long?>(null)
    private val syncLock = Mutex()

    override fun observeStatus(): Flow<HealthStatus> =
        combine(preferences.userPreferences, probe, lastSynced) { prefs, probe, synced ->
            HealthStatus(probe.availability, prefs.healthConnectEnabled, probe.granted, requiredPermissions, synced)
        }

    private suspend fun status(): HealthStatus = observeStatus().first()

    override suspend fun refresh() {
        val availability = gateway.availability()
        val granted = if (availability == HealthAvailability.AVAILABLE) runCatching { gateway.grantedPermissions() }.getOrDefault(emptySet()) else emptySet()
        probe.value = Probe(availability, granted)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        preferences.setHealthConnectEnabled(enabled)
        if (!enabled) steps.value = null
    }

    override suspend fun sync(): HealthSyncResult? = syncLock.withLock {
        if (!status().ready) return null
        val now = clock.instant()
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val since = now.minus(Duration.ofDays(SYNC_WINDOW_DAYS))
        // A fresh version every sync, so edits made here always replace the old copy.
        val version = now.toEpochMilli()

        val workoutRecords = workouts.observeHistory().first()
            .filter { it.endedAt >= since.toEpochMilli() }
            .map { workoutRecord(it, version, zone) }
        val weightRecords = body.observeWeights().first()
            .filter { it.healthConnectId == null && !it.date.isBefore(today.minusDays(SYNC_WINDOW_DAYS)) }
            .map { weightRecord(it, version, zone) }
        val mealRecords = (0 until SYNC_WINDOW_DAYS).flatMap { daysAgo ->
            val date = today.minusDays(daysAgo)
            val day = nutrition.observeDiary(date).first()
            MealType.entries.mapNotNull { meal -> mealRecord(date, meal, day.entriesFor(meal), version, zone) }
        }

        gateway.upsert(workoutRecords + weightRecords + mealRecords)
        val deleted = reconcile(KIND_WORKOUTS, ExerciseSessionRecord::class, workoutRecords) +
            reconcile(KIND_WEIGHTS, WeightRecord::class, weightRecords) +
            reconcile(KIND_MEALS, NutritionRecord::class, mealRecords)

        var imported = 0
        gateway.readWeightsFromOtherApps(since).forEach { weight ->
            val date = weight.time.atZone(weight.zoneOffset ?: zone).toLocalDate()
            if (body.importWeight(date, weight.kg, weight.id)) imported++
        }

        ledger.lastSyncedAt = now.toEpochMilli()
        lastSynced.value = ledger.lastSyncedAt
        HealthSyncResult(written = workoutRecords.size + weightRecords.size + mealRecords.size, deleted = deleted, importedWeights = imported)
    }

    /** Deletes what was written before but is gone here now; returns how many. */
    private suspend fun reconcile(kind: String, type: KClass<out Record>, current: List<Record>): Int {
        val ids = current.mapNotNull { it.metadata.clientRecordId }.toSet()
        val gone = ledger.written(kind) - ids
        if (gone.isNotEmpty()) gateway.deleteByClientIds(type, gone.toList())
        ledger.setWritten(kind, ids)
        return gone.size
    }

    override fun observeStepsToday(): Flow<Long?> = steps.asStateFlow()

    override suspend fun refreshSteps() {
        if (!status().ready) {
            steps.value = null
            return
        }
        val start = LocalDate.now(clock).atStartOfDay(clock.zone).toInstant()
        steps.update { runCatching { gateway.stepsBetween(start, clock.instant()) }.getOrNull() ?: it }
    }

    private companion object {
        const val KIND_WORKOUTS = "workouts"
        const val KIND_WEIGHTS = "weights"
        const val KIND_MEALS = "meals"
    }
}
