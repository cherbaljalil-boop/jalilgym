package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.exercise.ExerciseDraftError
import com.chdjalil.jalilgym.core.domain.exercise.defaultIncrementKg
import com.chdjalil.jalilgym.core.domain.exercise.validate
import com.chdjalil.jalilgym.core.domain.repository.ExerciseImageStore
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.ExerciseDraft
import com.chdjalil.jalilgym.core.model.MediaType
import com.chdjalil.jalilgym.core.model.MuscleRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class OfflineFirstExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val imageStore: ExerciseImageStore,
) : ExerciseRepository {

    override fun observeExercises(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeExercise(id: String): Flow<Exercise?> =
        exerciseDao.observeById(id).map { it?.asExternalModel() }

    override fun observeExerciseCount(): Flow<Int> = exerciseDao.observeCount()

    override suspend fun saveCustomExercise(draft: ExerciseDraft): String {
        val errors: Set<ExerciseDraftError> = draft.validate()
        require(errors.isEmpty()) { "Invalid exercise draft: $errors" }

        val now = System.currentTimeMillis()
        val existing = draft.id?.let { exerciseDao.getById(it) }
        require(existing?.exercise?.isBuiltIn != true) { "Built-in exercises can't be edited." }
        val id = existing?.exercise?.id ?: UUID.randomUUID().toString()

        val entity = ExerciseEntity(
            id = id,
            name = draft.name.trim(),
            aliases = existing?.exercise?.aliases.orEmpty(),
            instructions = draft.instructions.map(String::trim).filter(String::isNotEmpty),
            equipment = draft.equipment,
            mechanics = draft.mechanics,
            force = draft.force,
            difficulty = draft.difficulty,
            trackingType = draft.trackingType,
            defaultIncrementKg = defaultIncrementKg(
                draft.equipment,
                draft.mechanics,
                draft.primaryMuscles,
                draft.trackingType,
            ),
            isBuiltIn = false,
            createdAt = existing?.exercise?.createdAt ?: now,
            updatedAt = now,
        )
        val muscles = draft.primaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.PRIMARY) } +
            draft.secondaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.SECONDARY) }
        val images = listOfNotNull(draft.imagePath).mapIndexed { index, path ->
            ExerciseImageEntity("$id#$index", id, path, MediaType.IMAGE, index)
        }
        exerciseDao.replaceExercise(entity, muscles, images)

        // Remove a photo the user replaced or cleared.
        existing?.images?.map { it.uri }
            ?.filter { it != draft.imagePath }
            ?.forEach { imageStore.delete(it) }
        return id
    }

    override suspend fun deleteCustomExercise(id: String) {
        exerciseDao.softDeleteCustom(id, System.currentTimeMillis())
    }
}
