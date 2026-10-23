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
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgressionRule
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
class OfflineFirstProgramRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var database: JalilgymDatabase
    private lateinit var importer: SeedImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JalilgymDatabase::class.java).allowMainThreadQueries().build()
        importer = SeedImporter(context, database)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun TestScope.repository(): OfflineFirstProgramRepository {
        importer.importExercises(importer.readExerciseSeed())
        importer.importPrograms(importer.readProgramSeed())
        val dataStore = DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            scope = backgroundScope,
            produceFile = { File(tmpFolder.root, "prefs.json") },
        )
        return OfflineFirstProgramRepository(database.programDao(), database.exerciseDao(), JgPreferencesDataSource(dataStore))
    }

    @Test
    fun copyingATemplateCreatesAnIndependentProgram() = runTest {
        val repository = repository()

        val id = repository.copyProgram(ProgramTemplates.PPL_3, name = "My PPL")
        val copy = repository.observeProgram(id).first()!!
        val template = repository.observeProgram(ProgramTemplates.PPL_3).first()!!

        assertThat(copy.isTemplate).isFalse()
        assertThat(copy.name).isEqualTo("My PPL")
        assertThat(copy.sourceTemplateId).isEqualTo(ProgramTemplates.PPL_3)
        assertThat(copy.days.map { it.name }).isEqualTo(template.days.map { it.name })
        assertThat(copy.days.flatMap { d -> d.exercises.map { it.exercise.id } })
            .isEqualTo(template.days.flatMap { d -> d.exercises.map { it.exercise.id } })
        assertThat(copy.days.map { it.id }).containsNoneIn(template.days.map { it.id })

        repository.renameDay(copy.days.first().id, "Chest & shoulders")
        assertThat(repository.observeProgram(ProgramTemplates.PPL_3).first()!!.days.first().name).isEqualTo("Push")
        assertThat(repository.observeUserPrograms().first().map { it.id }).containsExactly(id)
        assertThat(repository.observeTemplates().first()).hasSize(3)
    }

    @Test
    fun editingDaysAndExercises() = runTest {
        val repository = repository()
        val id = repository.createProgram("Custom")
        val dayId = repository.observeProgram(id).first()!!.days.single().id

        repository.addExercises(dayId, listOf("barbell-back-squat", "leg-extension", "plank"))
        var day = repository.observeDay(dayId).first()!!
        assertThat(day.exercises.map { it.exercise.id }).containsExactly("barbell-back-squat", "leg-extension", "plank").inOrder()
        assertThat(day.exercises.last().progressionRule).isEqualTo(ProgressionRule.NONE)

        repository.moveExercise(day.exercises.last().id, -2)
        repository.toggleSupersetWithNext(dayId, 1)
        day = repository.observeDay(dayId).first()!!
        assertThat(day.exercises.map { it.exercise.id }).containsExactly("plank", "barbell-back-squat", "leg-extension").inOrder()
        assertThat(day.exercises.map { it.position }).containsExactly(0, 1, 2).inOrder()
        assertThat(day.exercises[1].supersetGroup).isNotNull()
        assertThat(day.exercises[1].supersetGroup).isEqualTo(day.exercises[2].supersetGroup)

        val squat = day.exercises[1]
        repository.updatePrescription(
            squat.id,
            Prescription(targetSets = 5, repMin = 5, repMax = 3, restSeconds = 240, incrementKg = 5.0),
        )
        val updated = repository.observeDay(dayId).first()!!.exercises[1]
        assertThat(updated.targetSets).isEqualTo(5)
        assertThat(updated.repMax).isEqualTo(5) // clamped to at least repMin

        repository.removeExercise(day.exercises[0].id)
        assertThat(repository.observeDay(dayId).first()!!.exercises.map { it.position }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun daysRotateAndStayConsistentWhenDeleted() = runTest {
        val repository = repository()
        val id = repository.copyProgram(ProgramTemplates.PPL_3)
        repository.activate(id)

        assertThat(repository.observeActiveProgram().first()!!.nextDay!!.name).isEqualTo("Push")
        repository.advanceRotation()
        repository.advanceRotation()
        assertThat(repository.observeActiveProgram().first()!!.nextDay!!.name).isEqualTo("Legs")
        repository.advanceRotation()
        assertThat(repository.observeActiveProgram().first()!!.nextDayIndex).isEqualTo(0)

        val days = repository.observeProgram(id).first()!!.days
        repository.duplicateDay(days[0].id)
        assertThat(repository.observeProgram(id).first()!!.days.map { it.name })
            .containsExactly("Push", "Push (copy)", "Pull", "Legs").inOrder()

        repository.moveDay(days[2].id, -3)
        assertThat(repository.observeProgram(id).first()!!.days.map { it.name })
            .containsExactly("Legs", "Push", "Push (copy)", "Pull").inOrder()

        repository.setNextDay(3)
        repository.deleteDay(repository.observeProgram(id).first()!!.days[3].id)
        val active = repository.observeActiveProgram().first()!!
        assertThat(active.program.days.map { it.dayIndex }).containsExactly(0, 1, 2).inOrder()
        assertThat(active.nextDayIndex).isEqualTo(0)

        repository.deleteProgram(id)
        assertThat(repository.observeActiveProgram().first()).isNull()
        assertThat(repository.observeUserPrograms().first()).isEmpty()
    }
}
