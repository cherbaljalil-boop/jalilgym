package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.model.AchievementCategory

/** Everything achievements are judged on, gathered from the database. */
data class AchievementStats(
    val workouts: Int = 0,
    val bestStreakWeeks: Int = 0,
    val records: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val bodyWeightKg: Double? = null,
    /** Best estimated 1RM by exercise id. */
    val bestOneRepMaxKg: Map<String, Double> = emptyMap(),
    val customPrograms: Int = 0,
    val customExercises: Int = 0,
    val photos: Int = 0,
    val mealsLogged: Int = 0,
    val loggingStreakDays: Int = 0,
    val macroDaysHit: Int = 0,
)

data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    val target: Double,
    /** Current progress toward [target], in the same unit. */
    val progress: (AchievementStats) -> Double,
)

private fun bodyweightMultiple(exerciseId: String, multiple: Double) = { stats: AchievementStats ->
    val bw = stats.bodyWeightKg
    val lift = stats.bestOneRepMaxKg[exerciseId]
    if (bw == null || bw <= 0 || lift == null) 0.0 else lift / (bw * multiple)
}

/** The badge catalog (docs/BLUEPRINT.md §3.6). Ids are stable: unlocks are stored by id. */
val ACHIEVEMENTS: List<AchievementDefinition> = listOf(
    AchievementDefinition("first_workout", AchievementCategory.MILESTONES, 1.0) { it.workouts.toDouble() },
    AchievementDefinition("workouts_10", AchievementCategory.MILESTONES, 10.0) { it.workouts.toDouble() },
    AchievementDefinition("workouts_50", AchievementCategory.MILESTONES, 50.0) { it.workouts.toDouble() },
    AchievementDefinition("workouts_100", AchievementCategory.MILESTONES, 100.0) { it.workouts.toDouble() },
    AchievementDefinition("workouts_250", AchievementCategory.MILESTONES, 250.0) { it.workouts.toDouble() },
    AchievementDefinition("streak_4", AchievementCategory.STREAKS, 4.0) { it.bestStreakWeeks.toDouble() },
    AchievementDefinition("streak_12", AchievementCategory.STREAKS, 12.0) { it.bestStreakWeeks.toDouble() },
    AchievementDefinition("streak_26", AchievementCategory.STREAKS, 26.0) { it.bestStreakWeeks.toDouble() },
    AchievementDefinition("streak_52", AchievementCategory.STREAKS, 52.0) { it.bestStreakWeeks.toDouble() },
    AchievementDefinition("first_pr", AchievementCategory.STRENGTH, 1.0) { it.records.toDouble() },
    AchievementDefinition("prs_10", AchievementCategory.STRENGTH, 10.0) { it.records.toDouble() },
    AchievementDefinition("bodyweight_bench", AchievementCategory.STRENGTH, 1.0, bodyweightMultiple("barbell-bench-press", 1.0)),
    AchievementDefinition("squat_1_5x", AchievementCategory.STRENGTH, 1.0, bodyweightMultiple("barbell-back-squat", 1.5)),
    AchievementDefinition("deadlift_2x", AchievementCategory.STRENGTH, 1.0, bodyweightMultiple("conventional-deadlift", 2.0)),
    AchievementDefinition("volume_10t", AchievementCategory.VOLUME, 10_000.0) { it.totalVolumeKg },
    AchievementDefinition("volume_100t", AchievementCategory.VOLUME, 100_000.0) { it.totalVolumeKg },
    AchievementDefinition("volume_1000t", AchievementCategory.VOLUME, 1_000_000.0) { it.totalVolumeKg },
    AchievementDefinition("first_meal", AchievementCategory.NUTRITION, 1.0) { it.mealsLogged.toDouble() },
    AchievementDefinition("logging_7", AchievementCategory.NUTRITION, 7.0) { it.loggingStreakDays.toDouble() },
    AchievementDefinition("logging_30", AchievementCategory.NUTRITION, 30.0) { it.loggingStreakDays.toDouble() },
    AchievementDefinition("macros_7", AchievementCategory.NUTRITION, 7.0) { it.macroDaysHit.toDouble() },
    AchievementDefinition("custom_program", AchievementCategory.EXPLORER, 1.0) { it.customPrograms.toDouble() },
    AchievementDefinition("custom_exercise", AchievementCategory.EXPLORER, 1.0) { it.customExercises.toDouble() },
    AchievementDefinition("first_photo", AchievementCategory.EXPLORER, 1.0) { it.photos.toDouble() },
)

/** Ids of achievements whose target is met by [stats]. */
fun earnedAchievements(stats: AchievementStats): Set<String> =
    ACHIEVEMENTS.filter { it.progress(stats) >= it.target }.map { it.id }.toSet()
