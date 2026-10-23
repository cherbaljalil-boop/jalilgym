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
        }
    }

    companion object {
        const val EXERCISES_ASSET = "seed/exercises.json"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
