package com.chdjalil.jalilgym.core.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType as HcMealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.testing.repository.FakeBodyRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeFoodRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeNutritionRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class FakeGateway(var availability: HealthAvailability = HealthAvailability.AVAILABLE) : HealthConnectGateway {
    var granted: Set<String> = HEALTH_PERMISSIONS
    val stored = mutableMapOf<String, Record>()
    val deleted = mutableListOf<Pair<KClass<out Record>, String>>()
    var otherAppWeights = listOf<ExternalWeight>()
    var steps: Long? = 4321

    override fun availability() = availability

    override suspend fun grantedPermissions() = granted

    override suspend fun upsert(records: List<Record>) = records.forEach { stored[it.metadata.clientRecordId!!] = it }

    override suspend fun deleteByClientIds(type: KClass<out Record>, clientRecordIds: List<String>) {
        clientRecordIds.forEach {
            stored.remove(it)
            deleted += type to it
        }
    }

    override suspend fun readWeightsFromOtherApps(since: Instant) = otherAppWeights

    override suspend fun stepsBetween(start: Instant, end: Instant) = steps
}

private class MemoryLedger : SyncLedger {
    private val map = mutableMapOf<String, Set<String>>()

    override fun written(kind: String) = map[kind].orEmpty()

    override fun setWritten(kind: String, ids: Set<String>) {
        map[kind] = ids
    }

    override var lastSyncedAt: Long? = null
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HealthSyncTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val now = Instant.parse("2026-10-24T18:00:00Z")
    private val clock = Clock.fixed(now, zone)
    private val today = LocalDate.now(clock)

    private val gateway = FakeGateway()
    private val prefs = FakeUserPreferencesRepository()
    private val workouts = FakeWorkoutRepository()
    private val body = FakeBodyRepository()
    private val foods = FakeFoodRepository()
    private val nutrition = FakeNutritionRepository(foods)
    private val repository = HealthConnectRepository(gateway, MemoryLedger(), clock, prefs, workouts, body, nutrition)

    private suspend fun ready() {
        repository.setEnabled(true)
        repository.refresh()
    }

    @Test
    fun nothingHappensUntilEnabledAvailableAndGranted() = runTest {
        repository.refresh()
        assertThat(repository.sync()).isNull()

        repository.setEnabled(true)
        gateway.granted = emptySet()
        repository.refresh()
        assertThat(repository.observeStatus().first().ready).isFalse()
        assertThat(repository.sync()).isNull()

        gateway.availability = HealthAvailability.NEEDS_INSTALL_OR_UPDATE
        repository.refresh()
        assertThat(repository.observeStatus().first().availability).isEqualTo(HealthAvailability.NEEDS_INSTALL_OR_UPDATE)
        assertThat(gateway.stored).isEmpty()
    }

    @Test
    fun writesWorkoutsWeightsAndMealsIdempotently() = runTest {
        ready()
        val start = now.minusSeconds(3 * 3600).toEpochMilli()
        workouts.now = start
        val id = workouts.startEmpty("Push")
        workouts.now = start + 3_600_000
        workouts.finish(id, notes = null)
        body.logWeight(today, 80.0)
        nutrition.quickAdd(today, MealType.BREAKFAST, "Oats", Nutrients(400.0, 20.0, 60.0, 8.0, sodiumMg = 500.0))
        nutrition.quickAdd(today, MealType.BREAKFAST, "Coffee", Nutrients(5.0, 0.0, 0.0, 0.0))

        val first = repository.sync()!!
        assertThat(first.written).isEqualTo(3)
        val session = gateway.stored.getValue("jalilgym-workout-$id") as ExerciseSessionRecord
        assertThat(session.exerciseType).isEqualTo(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING)
        assertThat(session.title).isEqualTo("Push")
        val meal = gateway.stored.values.filterIsInstance<NutritionRecord>().single()
        assertThat(meal.energy!!.inKilocalories).isWithin(1e-6).of(405.0)
        assertThat(meal.sodium!!.inGrams).isWithin(1e-9).of(0.5)
        assertThat(meal.mealType).isEqualTo(HcMealType.MEAL_TYPE_BREAKFAST)
        assertThat(meal.startTime.atZone(zone).toLocalTime().hour).isEqualTo(8)
        val weight = gateway.stored.values.filterIsInstance<WeightRecord>().single()
        assertThat(weight.weight.inKilograms).isEqualTo(80.0)

        // Syncing again rewrites the same IDs rather than adding copies.
        repository.sync()
        assertThat(gateway.stored).hasSize(3)
        assertThat(repository.observeStatus().first().lastSyncedAt).isEqualTo(now.toEpochMilli())
    }

    @Test
    fun deletionsHereAreDeletedThere() = runTest {
        ready()
        body.logWeight(today, 80.0)
        body.logWeight(today.minusDays(1), 80.4)
        repository.sync()
        assertThat(gateway.stored).hasSize(2)

        body.deleteWeight(body.weights.value.first().id)
        val result = repository.sync()!!

        assertThat(result.deleted).isEqualTo(1)
        assertThat(gateway.deleted.single().first).isEqualTo(WeightRecord::class)
        assertThat(gateway.stored).hasSize(1)
    }

    @Test
    fun importsOtherAppsWeightsWithoutEchoingThemBack() = runTest {
        ready()
        body.logWeight(today, 80.0)
        gateway.otherAppWeights = listOf(
            ExternalWeight("scale-1", today.minusDays(2).atTime(6, 30).atZone(zone).toInstant(), ZoneOffset.ofHours(2), 80.8),
            // A day that already has a weight here is left alone.
            ExternalWeight("scale-2", today.atTime(6, 30).atZone(zone).toInstant(), ZoneOffset.ofHours(2), 79.0),
        )

        val result = repository.sync()!!
        assertThat(result.importedWeights).isEqualTo(1)
        assertThat(body.weights.value.map { it.weightKg }).containsExactly(80.8, 80.0).inOrder()

        // The imported weight isn't written back as ours.
        repository.sync()
        assertThat(gateway.stored.values.filterIsInstance<WeightRecord>().map { it.weight.inKilograms }).containsExactly(80.0)
    }

    @Test
    fun stepsOnlyWhenReady() = runTest {
        repository.refreshSteps()
        assertThat(repository.observeStepsToday().first()).isNull()
        ready()
        repository.refreshSteps()
        assertThat(repository.observeStepsToday().first()).isEqualTo(4321)
        repository.setEnabled(false)
        assertThat(repository.observeStepsToday().first()).isNull()
    }
}
