package com.chdjalil.jalilgym.core.database.seed

import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseImageEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseMuscleEntity
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.MediaType
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.MuscleRole
import com.chdjalil.jalilgym.core.model.TrackingType
import kotlinx.serialization.Serializable

/** Shape of `assets/seed/exercises.json`. Bump [version] whenever the file changes. */
@Serializable
data class ExerciseSeedFile(
    val version: Int,
    val exercises: List<SeedExercise>,
)

@Serializable
data class SeedExercise(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val instructions: List<String>,
    val equipment: Equipment,
    val mechanics: Mechanics,
    val force: Force,
    val difficulty: Difficulty,
    val trackingType: TrackingType,
    val defaultIncrementKg: Double,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle> = emptyList(),
    /** Asset paths relative to `assets/`, e.g. `exercises/<id>/0.webp`. */
    val images: List<String> = emptyList(),
    /** Where the entry came from, for attribution (e.g. `free-exercise-db:<id>`). */
    val source: String? = null,
)

internal fun SeedExercise.toEntity(now: Long) = ExerciseEntity(
    id = id,
    name = name,
    aliases = aliases,
    instructions = instructions,
    equipment = equipment,
    mechanics = mechanics,
    force = force,
    difficulty = difficulty,
    trackingType = trackingType,
    defaultIncrementKg = defaultIncrementKg,
    isBuiltIn = true,
    createdAt = now,
    updatedAt = now,
)

internal fun SeedExercise.muscleEntities(): List<ExerciseMuscleEntity> =
    primaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.PRIMARY) } +
        secondaryMuscles.map { ExerciseMuscleEntity(id, it, MuscleRole.SECONDARY) }

internal fun SeedExercise.imageEntities(): List<ExerciseImageEntity> =
    images.mapIndexed { index, path ->
        ExerciseImageEntity(id = "$id#$index", exerciseId = id, uri = path, mediaType = MediaType.IMAGE, position = index)
    }
