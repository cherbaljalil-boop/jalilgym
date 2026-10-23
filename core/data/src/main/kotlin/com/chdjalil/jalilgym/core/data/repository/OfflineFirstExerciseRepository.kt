package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineFirstExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun observeExercises(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeExerciseCount(): Flow<Int> = exerciseDao.observeCount()
}
