package com.chdjalil.jalilgym.core.testing.data

import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType

fun sampleExercise(id: String, name: String = id) = Exercise(
    id = id,
    name = name,
    aliases = emptyList(),
    instructions = listOf("Do the movement."),
    equipment = Equipment.BARBELL,
    mechanics = Mechanics.COMPOUND,
    force = Force.PUSH,
    difficulty = Difficulty.BEGINNER,
    trackingType = TrackingType.WEIGHT_REPS,
    defaultIncrementKg = 2.5,
    primaryMuscles = listOf(Muscle.CHEST),
    secondaryMuscles = emptyList(),
    isBuiltIn = true,
)
