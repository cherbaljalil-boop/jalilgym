package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AchievementsAndVolumeTest {

    @Test
    fun `catalog ids are unique`() {
        assertThat(ACHIEVEMENTS.map { it.id }).containsNoDuplicates()
        assertThat(ACHIEVEMENTS).hasSize(24)
    }

    @Test
    fun `earned achievements follow the stats`() {
        val earned = earnedAchievements(
            AchievementStats(
                workouts = 12,
                bestStreakWeeks = 4,
                records = 1,
                totalVolumeKg = 15_000.0,
                bodyWeightKg = 80.0,
                bestOneRepMaxKg = mapOf("barbell-bench-press" to 85.0, "barbell-back-squat" to 110.0),
                customExercises = 1,
            ),
        )
        assertThat(earned).containsExactly(
            "first_workout", "workouts_10", "streak_4", "first_pr", "bodyweight_bench", "volume_10t", "custom_exercise",
        )
    }

    @Test
    fun `muscle sets weight secondary muscles by half`() {
        val bench = sampleExercise("bench").copy(primaryMuscles = listOf(Muscle.CHEST), secondaryMuscles = listOf(Muscle.TRICEPS))
        fun set(type: SetType, completed: Boolean = true) = WorkoutSet("x", 0, type, 80.0, 8, null, null, completed, 1L)
        val entry = SessionExercise(
            "e", bench, 0, null, null, null,
            sets = listOf(set(SetType.WARMUP), set(SetType.WORKING), set(SetType.WORKING), set(SetType.DROP), set(SetType.WORKING, completed = false)),
        )

        val sets = muscleSets(listOf(entry, entry))

        assertThat(sets).containsExactly(Muscle.CHEST, 6.0, Muscle.TRICEPS, 3.0)
        assertThat(sets.toHeat()[Muscle.CHEST]).isWithin(1e-6f).of(0.3f)
    }
}
