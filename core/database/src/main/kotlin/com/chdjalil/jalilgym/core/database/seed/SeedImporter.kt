package com.chdjalil.jalilgym.core.database.seed

import android.content.Context
import androidx.room.withTransaction
import com.chdjalil.jalilgym.core.database.JalilgymDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Loads bundled reference data from assets into Room. Built-in rows are
 * upserted, so re-importing a newer seed updates them in place without
 * touching user data that references them.
 */
class SeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: JalilgymDatabase,
) {
    fun readExerciseSeed(): ExerciseSeedFile =
        context.assets.open(EXERCISES_ASSET).bufferedReader().use { reader ->
            json.decodeFromString(ExerciseSeedFile.serializer(), reader.readText())
        }

    suspend fun importExercises(seed: ExerciseSeedFile, now: Long = System.currentTimeMillis()) {
        val dao = database.exerciseDao()
        database.withTransaction {
            dao.upsertExercises(seed.exercises.map { it.toEntity(now) })
            dao.deleteMusclesFor(seed.exercises.map { it.id })
            dao.insertMuscles(seed.exercises.flatMap { it.muscleEntities() })
            dao.deleteImagesFor(seed.exercises.map { it.id })
            dao.insertImages(seed.exercises.flatMap { it.imageEntities() })
        }
    }

    fun readProgramSeed(): ProgramSeedFile =
        context.assets.open(PROGRAMS_ASSET).bufferedReader().use { reader ->
            json.decodeFromString(ProgramSeedFile.serializer(), reader.readText())
        }

    /** Imports program templates. Exercises must be imported first (slots reference them). */
    suspend fun importPrograms(seed: ProgramSeedFile, now: Long = System.currentTimeMillis()) {
        val exerciseDao = database.exerciseDao()
        val programDao = database.programDao()
        database.withTransaction {
            seed.programs.forEach { program ->
                val increments = program.days.flatMap { it.exercises }.map { it.exerciseId }.distinct()
                    .associateWith { id -> exerciseDao.getById(id)?.exercise?.defaultIncrementKg ?: 2.5 }
                programDao.replaceProgramTree(
                    program.toEntity(now),
                    program.dayEntities(now),
                    program.slotEntities(now) { increments.getValue(it) },
                )
            }
        }
    }

    companion object {
        const val EXERCISES_ASSET = "seed/exercises.json"
        const val PROGRAMS_ASSET = "seed/programs.json"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
