package com.chdjalil.jalilgym.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.MediaType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.MuscleRole
import com.chdjalil.jalilgym.core.model.TrackingType
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
class ExerciseDaoTest {

    private lateinit var database: JalilgymDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeAll_returnsExercisesSortedWithMuscles() = runTest {
        dao.upsertExercises(listOf(exercise("squat", "Squat"), exercise("bench", "bench press")))
        dao.insertMuscles(
            listOf(
                ExerciseMuscleEntity("bench", Muscle.CHEST, MuscleRole.PRIMARY),
                ExerciseMuscleEntity("bench", Muscle.TRICEPS, MuscleRole.SECONDARY),
            ),
        )

        val result = dao.observeAll().first().map { it.asExternalModel() }

        assertThat(result.map { it.id }).containsExactly("bench", "squat").inOrder()
        assertThat(result[0].primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(result[0].secondaryMuscles).containsExactly(Muscle.TRICEPS)
        assertThat(result[0].aliases).containsExactly("alias")
    }

    @Test
    fun archivedAndDeletedExercisesAreHidden() = runTest {
        dao.upsertExercises(
            listOf(
                exercise("a", "A"),
                exercise("b", "B").copy(isArchived = true),
                exercise("c", "C").copy(deletedAt = 5L),
            ),
        )

        assertThat(dao.observeAll().first().map { it.exercise.id }).containsExactly("a")
        assertThat(dao.observeCount().first()).isEqualTo(1)
    }

    @Test
    fun deleteMusclesFor_keepsTheExercise() = runTest {
        dao.upsertExercises(listOf(exercise("bench", "Bench")))
        dao.insertMuscles(listOf(ExerciseMuscleEntity("bench", Muscle.CHEST, MuscleRole.PRIMARY)))

        dao.deleteMusclesFor(listOf("bench"))

        val bench = dao.getById("bench")!!
        assertThat(bench.muscles).isEmpty()
        assertThat(bench.exercise.name).isEqualTo("Bench")
    }

    @Test
    fun softDeleteOnlyAffectsCustomExercises() = runTest {
        dao.upsertExercises(listOf(exercise("custom", "Custom"), exercise("seed", "Seed").copy(isBuiltIn = true)))

        dao.softDeleteCustom("custom", now = 10L)
        dao.softDeleteCustom("seed", now = 10L)

        assertThat(dao.observeAll().first().map { it.exercise.id }).containsExactly("seed")
        // Deleted rows remain readable by id for history.
        assertThat(dao.observeById("custom").first()!!.exercise.deletedAt).isEqualTo(10L)
    }

    @Test
    fun imagesAreReturnedInPositionOrder() = runTest {
        dao.upsertExercises(listOf(exercise("bench", "Bench")))
        dao.insertImages(
            listOf(
                ExerciseImageEntity("b1", "bench", "exercises/bench/1.webp", MediaType.IMAGE, position = 1),
                ExerciseImageEntity("b0", "bench", "exercises/bench/0.webp", MediaType.IMAGE, position = 0),
            ),
        )

        val model = dao.observeById("bench").first()!!.asExternalModel()

        assertThat(model.images).containsExactly("exercises/bench/0.webp", "exercises/bench/1.webp").inOrder()
    }

    private fun exercise(id: String, name: String) = ExerciseEntity(
        id = id,
        name = name,
        aliases = listOf("alias"),
        instructions = listOf("Step one"),
        equipment = Equipment.BARBELL,
        mechanics = Mechanics.COMPOUND,
        force = Force.PUSH,
        difficulty = Difficulty.BEGINNER,
        trackingType = TrackingType.WEIGHT_REPS,
        defaultIncrementKg = 2.5,
        isBuiltIn = false,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
