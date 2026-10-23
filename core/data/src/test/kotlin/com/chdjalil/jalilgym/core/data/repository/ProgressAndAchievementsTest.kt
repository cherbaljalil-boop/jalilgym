package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.domain.progress.weekStart
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.PrType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ProgressAndAchievementsTest {

    private lateinit var database: JalilgymDatabase
    private lateinit var workouts: OfflineFirstWorkoutRepository
    private lateinit var progress: OfflineFirstProgressRepository
    private lateinit var achievements: OfflineFirstAchievementRepository
    private lateinit var body: OfflineFirstBodyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
        achievements = OfflineFirstAchievementRepository(database.progressDao(), database.bodyDao(), database.programDao())
        workouts = OfflineFirstWorkoutRepository(
            database.workoutDao(),
            database.programDao(),
            database.exerciseDao(),
            database.progressDao(),
            achievements,
        )
        progress = OfflineFirstProgressRepository(database.progressDao(), database.exerciseDao(), database.programDao())
        body = OfflineFirstBodyRepository(context, database.bodyDao(), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seed() {
        val importer = SeedImporter(ApplicationProvider.getApplicationContext(), database)
        importer.importExercises(importer.readExerciseSeed())
    }

    /** Logs one bench workout of [sets] × ([weight] kg × [reps]) and finishes it. */
    private suspend fun benchWorkout(weight: Double, reps: Int, sets: Int = 3) = run {
        val id = workouts.startEmpty("Bench day")
        workouts.addExercises(id, listOf("barbell-bench-press"))
        val exercise = workouts.observeSession(id).first()!!.exercises.single()
        exercise.sets.take(sets).forEach { set ->
            workouts.editSet(set.id) { it.copy(weightKg = weight, reps = reps) }
            workouts.setCompleted(set.id, true)
        }
        workouts.finish(id, notes = null)
    }

    @Test
    fun firstWorkoutStoresBaselinesAndUnlocksFirstWorkout() = runTest {
        seed()
        val summary = benchWorkout(weight = 60.0, reps = 8)

        // Baselines are stored but not celebrated.
        assertThat(summary.records).isEmpty()
        assertThat(progress.observeRecords("barbell-bench-press").first().map { it.type })
            .containsExactly(PrType.EST_1RM, PrType.MAX_WEIGHT, PrType.MAX_VOLUME)
        assertThat(summary.newAchievements).contains("first_workout")
        assertThat(summary.newAchievements).doesNotContain("first_pr")

        val status = achievements.observeAchievements().first().associateBy { it.id }
        assertThat(status.getValue("first_workout").isUnlocked).isTrue()
        assertThat(status.getValue("workouts_10").isUnlocked).isFalse()
        assertThat(status.getValue("workouts_10").progress).isEqualTo(1.0)
        // Unlocks are recorded once.
        assertThat(achievements.syncUnlocks()).isEmpty()
    }

    @Test
    fun heavierWorkoutSetsRecordsAndUnlocksFirstPr() = runTest {
        seed()
        benchWorkout(weight = 60.0, reps = 8)
        val summary = benchWorkout(weight = 70.0, reps = 8)

        val records = summary.records.associateBy { it.type }
        assertThat(records.keys).containsExactly(PrType.EST_1RM, PrType.MAX_WEIGHT, PrType.MAX_VOLUME)
        assertThat(records.getValue(PrType.MAX_WEIGHT).previousValue).isEqualTo(60.0)
        assertThat(records.getValue(PrType.MAX_WEIGHT).value).isEqualTo(70.0)
        assertThat(summary.newAchievements).contains("first_pr")
        assertThat(workouts.observeSummary(summary.session.id).first()!!.records).hasSize(3)

        // A lighter workout sets nothing.
        assertThat(benchWorkout(weight = 50.0, reps = 5).records).isEmpty()

        val series = progress.observeStrengthSeries("barbell-bench-press").first()
        assertThat(series.map { it.topWeightKg }).containsExactly(60.0, 70.0, 50.0).inOrder()
        assertThat(series[1].volumeKg).isEqualTo(70.0 * 8 * 3)

        val bench = progress.observeExerciseProgress().first().single()
        assertThat(bench.sessionCount).isEqualTo(3)
        assertThat(bench.bestEstimatedOneRepMaxKg).isEqualTo(series[1].bestEstimatedOneRepMaxKg)

        val muscles = progress.observeWeeklyMuscleSets(LocalDate.now().weekStart()).first()
        assertThat(muscles[Muscle.CHEST]).isEqualTo(9.0)

        val streak = progress.observeTrainingStreak().first()
        assertThat(streak.thisWeekWorkouts).isEqualTo(3)
        assertThat(streak.currentWeeks).isEqualTo(1)
    }

    @Test
    fun deletingAWorkoutRemovesItsRecords() = runTest {
        seed()
        val summary = benchWorkout(weight = 60.0, reps = 8)
        workouts.deleteSession(summary.session.id)
        assertThat(progress.observeRecords().first()).isEmpty()
    }

    @Test
    fun bodyLogsKeepOneEntryPerDay() = runTest {
        val today = LocalDate.now()
        body.logWeight(today.minusDays(1), 81.0)
        body.logWeight(today, 80.0)
        body.logWeight(today, 79.5)
        assertThat(body.observeWeights().first().map { it.weightKg }).containsExactly(81.0, 79.5).inOrder()

        body.logMeasurement(today, MeasurementType.WAIST, 84.0)
        body.logMeasurement(today, MeasurementType.WAIST, 83.0)
        body.logMeasurement(today, MeasurementType.CHEST, 104.0)
        assertThat(body.observeMeasurements().first().map { it.value }).containsExactly(83.0, 104.0)

        body.deleteWeight(body.observeWeights().first().first().id)
        assertThat(body.observeWeights().first().single().weightKg).isEqualTo(79.5)
    }
}
