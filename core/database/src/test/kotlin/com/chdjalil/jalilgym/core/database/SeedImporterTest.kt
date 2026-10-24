package com.chdjalil.jalilgym.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SeedImporterTest {

    private lateinit var database: JalilgymDatabase
    private lateinit var importer: SeedImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = SeedImporter(context, database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun bundledSeedIsValid() {
        val seed = importer.readExerciseSeed()

        assertThat(seed.version).isAtLeast(1)
        assertThat(seed.exercises.size).isAtLeast(95)
        assertThat(seed.exercises.map { it.id }).containsNoDuplicates()
        val context = ApplicationProvider.getApplicationContext<Context>()
        seed.exercises.forEach { exercise ->
            assertThat(exercise.primaryMuscles).isNotEmpty()
            assertThat(exercise.instructions).isNotEmpty()
            // A muscle is either primary or secondary for a given exercise, never both.
            assertThat(exercise.primaryMuscles.intersect(exercise.secondaryMuscles.toSet())).isEmpty()
            assertThat(exercise.id).matches("[a-z0-9-]+")
            assertThat(exercise.images).isNotEmpty()
            exercise.images.forEach { path ->
                // Throws if the bundled image is missing.
                context.assets.open(path).close()
            }
        }
    }

    @Test
    fun importIsIdempotent() = runTest {
        val seed = importer.readExerciseSeed()

        importer.importExercises(seed, now = 1L)
        importer.importExercises(seed, now = 2L)

        val dao = database.exerciseDao()
        assertThat(dao.observeCount().first()).isEqualTo(seed.exercises.size)
        val bench = dao.getById("barbell-bench-press")!!
        assertThat(bench.exercise.isBuiltIn).isTrue()
        assertThat(bench.exercise.updatedAt).isEqualTo(2L)
        val expectedMuscles = seed.exercises.first { it.id == "barbell-bench-press" }
            .let { it.primaryMuscles.size + it.secondaryMuscles.size }
        assertThat(bench.muscles).hasSize(expectedMuscles)
        assertThat(bench.asExternalModel().images)
            .containsExactly("exercises/barbell-bench-press/0.webp", "exercises/barbell-bench-press/1.webp")
            .inOrder()
    }

    @Test
    fun programTemplatesReferenceSeededExercises() = runTest {
        val exercises = importer.readExerciseSeed()
        val programs = importer.readProgramSeed()
        val exerciseIds = exercises.exercises.map { it.id }.toSet()

        assertThat(programs.programs.map { it.id })
            .containsExactly(
                "tpl-full-body-3", "tpl-ppl-3", "tpl-ppl-6",
                "tpl-upper-lower-4", "tpl-strength-3", "tpl-home-dumbbell-3", "tpl-bodyweight-3",
            )
        programs.programs.forEach { program ->
            assertThat(program.days).isNotEmpty()
            program.days.forEach { day ->
                assertThat(day.exercises).isNotEmpty()
                day.exercises.forEach { slot ->
                    assertThat(exerciseIds).contains(slot.exerciseId)
                    assertThat(slot.repMin).isAtMost(slot.repMax)
                }
                // Superset members must be next to each other.
                val groups = day.exercises.mapNotNull { it.supersetGroup }.distinct()
                groups.forEach { group ->
                    val positions = day.exercises.indices.filter { day.exercises[it].supersetGroup == group }
                    assertThat(positions.size).isAtLeast(2)
                    assertThat(positions.last() - positions.first()).isEqualTo(positions.size - 1)
                }
            }
        }

        importer.importExercises(exercises, now = 1L)
        importer.importPrograms(programs, now = 1L)
        importer.importPrograms(programs, now = 2L)

        val ppl = database.programDao().getProgram("tpl-ppl-3")!!.asExternalModel()
        assertThat(ppl.isTemplate).isTrue()
        assertThat(ppl.days.map { it.name }).containsExactly("Push", "Pull", "Legs").inOrder()
        assertThat(ppl.days.first().exercises.first().exercise.id).isEqualTo("barbell-bench-press")
    }
}
