package com.chdjalil.jalilgym.core.ui

import androidx.annotation.StringRes
import com.chdjalil.jalilgym.core.model.AchievementCategory
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.PrType

@get:StringRes
val PrType.labelRes: Int
    get() = when (this) {
        PrType.EST_1RM -> R.string.core_ui_pr_e1rm
        PrType.MAX_WEIGHT -> R.string.core_ui_pr_max_weight
        PrType.MAX_REPS_AT_WEIGHT -> R.string.core_ui_pr_max_reps
        PrType.MAX_VOLUME -> R.string.core_ui_pr_volume
    }

@get:StringRes
val MeasurementType.labelRes: Int
    get() = when (this) {
        MeasurementType.NECK -> R.string.core_ui_measure_neck
        MeasurementType.CHEST -> R.string.core_ui_measure_chest
        MeasurementType.WAIST -> R.string.core_ui_measure_waist
        MeasurementType.HIPS -> R.string.core_ui_measure_hips
        MeasurementType.ARM -> R.string.core_ui_measure_arm
        MeasurementType.THIGH -> R.string.core_ui_measure_thigh
        MeasurementType.CALF -> R.string.core_ui_measure_calf
        MeasurementType.BODY_FAT -> R.string.core_ui_measure_body_fat
    }

@get:StringRes
val PhotoPose.labelRes: Int
    get() = when (this) {
        PhotoPose.FRONT -> R.string.core_ui_pose_front
        PhotoPose.SIDE -> R.string.core_ui_pose_side
        PhotoPose.BACK -> R.string.core_ui_pose_back
    }

@get:StringRes
val AchievementCategory.labelRes: Int
    get() = when (this) {
        AchievementCategory.MILESTONES -> R.string.core_ui_badge_cat_milestones
        AchievementCategory.STREAKS -> R.string.core_ui_badge_cat_streaks
        AchievementCategory.STRENGTH -> R.string.core_ui_badge_cat_strength
        AchievementCategory.VOLUME -> R.string.core_ui_badge_cat_volume
        AchievementCategory.NUTRITION -> R.string.core_ui_badge_cat_nutrition
        AchievementCategory.EXPLORER -> R.string.core_ui_badge_cat_explorer
    }

/** Title and description of a badge, by achievement id (see core:domain ACHIEVEMENTS). */
fun achievementText(id: String): Pair<Int, Int> = when (id) {
    "first_workout" -> R.string.core_ui_badge_first_workout to R.string.core_ui_badge_first_workout_desc
    "workouts_10" -> R.string.core_ui_badge_workouts_10 to R.string.core_ui_badge_workouts_10_desc
    "workouts_50" -> R.string.core_ui_badge_workouts_50 to R.string.core_ui_badge_workouts_50_desc
    "workouts_100" -> R.string.core_ui_badge_workouts_100 to R.string.core_ui_badge_workouts_100_desc
    "workouts_250" -> R.string.core_ui_badge_workouts_250 to R.string.core_ui_badge_workouts_250_desc
    "streak_4" -> R.string.core_ui_badge_streak_4 to R.string.core_ui_badge_streak_4_desc
    "streak_12" -> R.string.core_ui_badge_streak_12 to R.string.core_ui_badge_streak_12_desc
    "streak_26" -> R.string.core_ui_badge_streak_26 to R.string.core_ui_badge_streak_26_desc
    "streak_52" -> R.string.core_ui_badge_streak_52 to R.string.core_ui_badge_streak_52_desc
    "first_pr" -> R.string.core_ui_badge_first_pr to R.string.core_ui_badge_first_pr_desc
    "prs_10" -> R.string.core_ui_badge_prs_10 to R.string.core_ui_badge_prs_10_desc
    "bodyweight_bench" -> R.string.core_ui_badge_bodyweight_bench to R.string.core_ui_badge_bodyweight_bench_desc
    "squat_1_5x" -> R.string.core_ui_badge_squat_1_5x to R.string.core_ui_badge_squat_1_5x_desc
    "deadlift_2x" -> R.string.core_ui_badge_deadlift_2x to R.string.core_ui_badge_deadlift_2x_desc
    "volume_10t" -> R.string.core_ui_badge_volume_10t to R.string.core_ui_badge_volume_10t_desc
    "volume_100t" -> R.string.core_ui_badge_volume_100t to R.string.core_ui_badge_volume_100t_desc
    "volume_1000t" -> R.string.core_ui_badge_volume_1000t to R.string.core_ui_badge_volume_1000t_desc
    "first_meal" -> R.string.core_ui_badge_first_meal to R.string.core_ui_badge_first_meal_desc
    "logging_7" -> R.string.core_ui_badge_logging_7 to R.string.core_ui_badge_logging_7_desc
    "logging_30" -> R.string.core_ui_badge_logging_30 to R.string.core_ui_badge_logging_30_desc
    "macros_7" -> R.string.core_ui_badge_macros_7 to R.string.core_ui_badge_macros_7_desc
    "custom_program" -> R.string.core_ui_badge_custom_program to R.string.core_ui_badge_custom_program_desc
    "custom_exercise" -> R.string.core_ui_badge_custom_exercise to R.string.core_ui_badge_custom_exercise_desc
    "first_photo" -> R.string.core_ui_badge_first_photo to R.string.core_ui_badge_first_photo_desc
    else -> R.string.core_ui_badge_unknown to R.string.core_ui_badge_unknown_desc
}
