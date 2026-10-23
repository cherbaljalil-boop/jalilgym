package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.datastore.UserPreferencesSerializer
import com.chdjalil.jalilgym.core.domain.program.ProgramTemplates
import com.chdjalil.jalilgym.core.model.ProgressionOutcome
import com.chdjalil.jalilgym.core.model.SetType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OfflineFirstWorkoutRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var database: JalilgymDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun TestScope.repositories(): Pair<OfflineFirstProgramRepository, OfflineFirstWorkoutRepository> {
        val importer = SeedImporter(ApplicationProvider.getApplicationContext(), database)
        importer.importExercises(importer.readExerciseSeed())
        importer.importPrograms(importer.readProgramSeed())
        val prefs = JgPreferencesDataSource(
            DataStoreFactory.create(UserPreferencesSerializer, scope = backgroundScope) { File(tmpFolder.root, "p.json") },
        )
        val programs = OfflineFirstProgramRepository(database.programDao(), database.exerciseDao(), prefs)
        val achievements = OfflineFirstAchievementRepository(
            database.progressDao(),
            database.bodyDao(),
            database.programDao(),
            database.foodLogDao(),
            database.dietPlanDao(),
        )
        val workouts = OfflineFirstWorkoutRepository(
            database.workoutDao(),
            database.programDao(),
            database.exerciseDao(),
            database.progressDao(),
            achievements,
        )
        return programs to workouts
    }

    @Test
    fun fullWorkoutLoopAppliesProgressionAndRotation() = runTest {
        val (programs, workouts) = repositories()
        val programId = programs.copyProgram(ProgramTemplates.PPL_3)
        programs.activate(programId)
        val push = programs.observeActiveProgram().first()!!.nextDay!!

        val sessionId = workouts.startFromDay(push.id)
        // Starting again while active resumes the same workout.
        assertThat(workouts.startFromDay(push.id)).isEqualTo(sessionId)

        var session = workouts.observeActiveSession().first()!!
        assertThat(session.name).isEqualTo("Push")
        val bench = session.exercises.first()
        assertThat(bench.exercise.id).isEqualTo("barbell-bench-press")
        assertThat(bench.sets).hasSize(bench.target!!.targetSets)
        assertThat(bench.sets.first().weightKg).isNull()
        assertThat(bench.sets.first().reps).isEqualTo(bench.target!!.repMin)

        // Hit the top of the range on every bench set; leave everything else undone.
        bench.sets.forEach { set ->
            workouts.editSet(set.id) { it.copy(weightKg = 80.0) }
            workouts.editSet(set.id) { it.copy(reps = bench.target!!.repMax) }
            workouts.setCompleted(set.id, true)
        }
        workouts.addWarmups(bench.id, listOf(20.0 to 10, 40.0 to 8))
        session = workouts.observeActiveSession().first()!!
        assertThat(session.exercises.first().sets.take(2).map { it.type }).containsExactly(SetType.WARMUP, SetType.WARMUP)
        session.exercises.first().sets.take(2).forEach { workouts.setCompleted(it.id, true) }

        val summary = workouts.finish(sessionId, notes = "  felt strong ")

        assertThat(summary.session.notes).isEqualTo("felt strong")
        assertThat(summary.session.exercises.map { it.exercise.id }).containsExactly("barbell-bench-press")
        assertThat(summary.completedSets).isEqualTo(bench.sets.size + 2)
        assertThat(summary.totalVolumeKg).isEqualTo(80.0 * bench.target!!.repMax * bench.sets.size)
        val note = summary.progression.single()
        assertThat(note.outcome).isEqualTo(ProgressionOutcome.INCREASE)
        assertThat(note.toWeightKg).isEqualTo(82.5)
        assertThat(workouts.observeActiveSession().first()).isNull()
        assertThat(programs.observeActiveProgram().first()!!.nextDay!!.name).isEqualTo("Pull")

        val history = workouts.observeHistory().first().single()
        assertThat(history.id).isEqualTo(sessionId)
        assertThat(history.exerciseCount).isEqualTo(1)
        assertThat(workouts.observeSummary(sessionId).first()!!.progression).hasSize(1)

        // Next push day: bench is pre-filled from the progression target, and shows last time's sets.
        val next = workouts.startFromDay(push.id)
        val nextBench = workouts.observeSession(next).first()!!.exercises.first()
        assertThat(nextBench.sets.map { it.weightKg }.distinct()).containsExactly(82.5)
        assertThat(nextBench.sets.first().reps).isEqualTo(nextBench.target!!.repMin)
        assertThat(nextBench.previous.filter { it.type == SetType.WORKING }.map { it.weightKg }.distinct()).containsExactly(80.0)
    }

    @Test
    fun emptyWorkoutsTakeAdHocExercisesAndCanBeDiscarded() = runTest {
        val (_, workouts) = repositories()
        val id = workouts.startEmpty("Quick session")

        workouts.addExercises(id, listOf("pull-up", "plank"))
        var session = workouts.observeActiveSession().first()!!
        assertThat(session.exercises.map { it.exercise.id }).containsExactly("pull-up", "plank").inOrder()
        assertThat(session.exercises.first().sets).hasSize(3)

        val added = workouts.addSet(session.exercises.first().id)
        workouts.removeSet(session.exercises.first().sets.first().id)
        workouts.swapExercise(session.exercises.last().id, "side-plank")
        session = workouts.observeActiveSession().first()!!
        assertThat(session.exercises.first().sets.map { it.position }).containsExactly(0, 1, 2).inOrder()
        assertThat(session.exercises.first().sets.last().id).isEqualTo(added)
        assertThat(session.exercises.last().exercise.id).isEqualTo("side-plank")

        workouts.removeExercise(session.exercises.first().id)
        assertThat(workouts.observeActiveSession().first()!!.exercises.single().position).isEqualTo(0)

        workouts.discard(id)
        assertThat(workouts.observeActiveSession().first()).isNull()
        assertThat(workouts.observeHistory().first()).isEmpty()
    }
}
