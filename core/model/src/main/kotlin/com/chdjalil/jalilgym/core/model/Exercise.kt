package com.chdjalil.jalilgym.core.model

data class Exercise(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val instructions: List<String>,
    val equipment: Equipment,
    val mechanics: Mechanics,
    val force: Force,
    val difficulty: Difficulty,
    val trackingType: TrackingType,
    val defaultIncrementKg: Double,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val isBuiltIn: Boolean,
    /**
     * Demonstration images in display order: asset paths (`exercises/<id>/0.webp`)
     * for built-in exercises, absolute file paths for custom ones.
     */
    val images: List<String> = emptyList(),
)

/** User input for creating or editing a custom exercise. */
data class ExerciseDraft(
    val id: String? = null,
    val name: String = "",
    val instructions: List<String> = emptyList(),
    val equipment: Equipment = Equipment.BARBELL,
    val mechanics: Mechanics = Mechanics.COMPOUND,
    val force: Force = Force.PUSH,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val primaryMuscles: Set<Muscle> = emptySet(),
    val secondaryMuscles: Set<Muscle> = emptySet(),
    val imagePath: String? = null,
)

/** Muscle regions, matching the body heatmap. */
enum class Muscle {
    CHEST,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    TRAPS,
    LATS,
    LOWER_BACK,
    ABS,
    OBLIQUES,
    GLUTES,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    CALVES,
}

enum class MuscleRole { PRIMARY, SECONDARY }

enum class Equipment {
    BARBELL,
    DUMBBELL,
    EZ_BAR,
    MACHINE,
    CABLE,
    SMITH_MACHINE,
    KETTLEBELL,
    BANDS,
    BODYWEIGHT,
    OTHER,
}

enum class Mechanics { COMPOUND, ISOLATION }

enum class Force { PUSH, PULL, STATIC }

enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

enum class TrackingType { WEIGHT_REPS, REPS, TIME, DISTANCE }
