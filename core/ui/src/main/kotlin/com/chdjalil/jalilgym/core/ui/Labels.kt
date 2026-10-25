package com.chdjalil.jalilgym.core.ui

import androidx.annotation.StringRes
import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.TrackingType

// Display names for model enums, shared by every feature.

@get:StringRes
val Muscle.labelRes: Int
    get() = when (this) {
        Muscle.CHEST -> R.string.core_ui_muscle_chest
        Muscle.FRONT_DELTS -> R.string.core_ui_muscle_front_delts
        Muscle.SIDE_DELTS -> R.string.core_ui_muscle_side_delts
        Muscle.REAR_DELTS -> R.string.core_ui_muscle_rear_delts
        Muscle.BICEPS -> R.string.core_ui_muscle_biceps
        Muscle.TRICEPS -> R.string.core_ui_muscle_triceps
        Muscle.FOREARMS -> R.string.core_ui_muscle_forearms
        Muscle.TRAPS -> R.string.core_ui_muscle_traps
        Muscle.LATS -> R.string.core_ui_muscle_lats
        Muscle.LOWER_BACK -> R.string.core_ui_muscle_lower_back
        Muscle.ABS -> R.string.core_ui_muscle_abs
        Muscle.OBLIQUES -> R.string.core_ui_muscle_obliques
        Muscle.GLUTES -> R.string.core_ui_muscle_glutes
        Muscle.QUADS -> R.string.core_ui_muscle_quads
        Muscle.HAMSTRINGS -> R.string.core_ui_muscle_hamstrings
        Muscle.ADDUCTORS -> R.string.core_ui_muscle_adductors
        Muscle.CALVES -> R.string.core_ui_muscle_calves
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.BARBELL -> R.string.core_ui_equipment_barbell
        Equipment.DUMBBELL -> R.string.core_ui_equipment_dumbbell
        Equipment.EZ_BAR -> R.string.core_ui_equipment_ez_bar
        Equipment.MACHINE -> R.string.core_ui_equipment_machine
        Equipment.CABLE -> R.string.core_ui_equipment_cable
        Equipment.SMITH_MACHINE -> R.string.core_ui_equipment_smith_machine
        Equipment.KETTLEBELL -> R.string.core_ui_equipment_kettlebell
        Equipment.BANDS -> R.string.core_ui_equipment_bands
        Equipment.BODYWEIGHT -> R.string.core_ui_equipment_bodyweight
        Equipment.OTHER -> R.string.core_ui_equipment_other
    }

@get:StringRes
val Mechanics.labelRes: Int
    get() = when (this) {
        Mechanics.COMPOUND -> R.string.core_ui_mechanics_compound
        Mechanics.ISOLATION -> R.string.core_ui_mechanics_isolation
    }

@get:StringRes
val Force.labelRes: Int
    get() = when (this) {
        Force.PUSH -> R.string.core_ui_force_push
        Force.PULL -> R.string.core_ui_force_pull
        Force.STATIC -> R.string.core_ui_force_static
    }

@get:StringRes
val Difficulty.labelRes: Int
    get() = when (this) {
        Difficulty.BEGINNER -> R.string.core_ui_level_beginner
        Difficulty.INTERMEDIATE -> R.string.core_ui_level_intermediate
        Difficulty.ADVANCED -> R.string.core_ui_level_advanced
    }

@get:StringRes
val ExperienceLevel.labelRes: Int
    get() = when (this) {
        ExperienceLevel.BEGINNER -> R.string.core_ui_level_beginner
        ExperienceLevel.INTERMEDIATE -> R.string.core_ui_level_intermediate
        ExperienceLevel.ADVANCED -> R.string.core_ui_level_advanced
    }

@get:StringRes
val TrackingType.labelRes: Int
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> R.string.core_ui_tracking_weight_reps
        TrackingType.REPS -> R.string.core_ui_tracking_reps
        TrackingType.TIME -> R.string.core_ui_tracking_time
        TrackingType.DISTANCE -> R.string.core_ui_tracking_distance
    }

@get:StringRes
val Goal.labelRes: Int
    get() = when (this) {
        Goal.BUILD_MUSCLE -> R.string.core_ui_goal_build_muscle
        Goal.LOSE_FAT -> R.string.core_ui_goal_lose_fat
        Goal.GET_STRONGER -> R.string.core_ui_goal_get_stronger
        Goal.GENERAL_FITNESS -> R.string.core_ui_goal_general_fitness
        Goal.ENDURANCE -> R.string.core_ui_goal_endurance
    }

@get:StringRes
val StarterProgram.labelRes: Int
    get() = when (this) {
        StarterProgram.FULL_BODY_3 -> R.string.core_ui_program_full_body_3
        StarterProgram.PPL_3 -> R.string.core_ui_program_ppl_3
        StarterProgram.PPL_6 -> R.string.core_ui_program_ppl_6
        StarterProgram.CUSTOM -> R.string.core_ui_program_custom
    }
