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
