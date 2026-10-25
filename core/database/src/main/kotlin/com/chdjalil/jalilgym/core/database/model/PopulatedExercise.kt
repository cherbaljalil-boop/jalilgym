package com.chdjalil.jalilgym.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.MuscleRole

data class PopulatedExercise(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val muscles: List<ExerciseMuscleEntity>,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val images: List<ExerciseImageEntity> = emptyList(),
)

fun PopulatedExercise.asExternalModel() = Exercise(
    id = exercise.id,
    name = exercise.name,
    aliases = exercise.aliases,
    instructions = exercise.instructions,
    equipment = exercise.equipment,
    mechanics = exercise.mechanics,
    force = exercise.force,
    difficulty = exercise.difficulty,
    trackingType = exercise.trackingType,
    defaultIncrementKg = exercise.defaultIncrementKg,
    primaryMuscles = muscles.filter { it.role == MuscleRole.PRIMARY }.map { it.muscle }.sorted(),
    secondaryMuscles = muscles.filter { it.role == MuscleRole.SECONDARY }.map { it.muscle }.sorted(),
    isBuiltIn = exercise.isBuiltIn,
    images = images.sortedBy { it.position }.map { it.uri },
)
