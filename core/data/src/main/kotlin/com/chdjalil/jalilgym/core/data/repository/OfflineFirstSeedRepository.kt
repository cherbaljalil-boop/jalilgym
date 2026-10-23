package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import com.chdjalil.jalilgym.core.database.seed.SeedImporter
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.domain.repository.SeedRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class OfflineFirstSeedRepository @Inject constructor(
    private val importer: SeedImporter,
    private val preferences: JgPreferencesDataSource,
    @Dispatcher(JgDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : SeedRepository {

    private val mutex = Mutex()

    override suspend fun seedIfNeeded() = mutex.withLock {
        withContext(ioDispatcher) {
            val exercises = importer.readExerciseSeed()
            if (exercises.version > preferences.installedSeedVersion()) {
                importer.importExercises(exercises)
                preferences.setInstalledSeedVersion(exercises.version)
            }
            // Templates reference exercises, so they're imported second.
            val programs = importer.readProgramSeed()
            if (programs.version > preferences.installedProgramSeedVersion()) {
                importer.importPrograms(programs)
                preferences.setInstalledProgramSeedVersion(programs.version)
            }
            val foods = importer.readFoodSeed()
            if (foods.version > preferences.installedFoodSeedVersion()) {
                importer.importFoods(foods)
                preferences.setInstalledFoodSeedVersion(foods.version)
            }
        }
    }
}
