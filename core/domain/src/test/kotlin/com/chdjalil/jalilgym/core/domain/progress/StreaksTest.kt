package com.chdjalil.jalilgym.core.domain.progress

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class StreaksTest {

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 21)
    private fun weeksAgo(weeks: Long, day: Long = 0) = today.weekStart().minusWeeks(weeks).plusDays(day)

    /** [count] workouts in the week [weeks] ago. */
    private fun week(weeks: Long, count: Int) = (0 until count).map { weeksAgo(weeks, it.toLong()) }

    @Test
    fun `counts consecutive weeks on target and the current week once reached`() {
        val dates = week(3, 3) + week(2, 3) + week(1, 3) + listOf(today)
        val streak = trainingStreak(dates, weeklyTarget = 3, today = today)
        assertThat(streak.currentWeeks).isEqualTo(3)
        assertThat(streak.thisWeekWorkouts).isEqualTo(1)

        val done = trainingStreak(dates + listOf(today, today), weeklyTarget = 3, today = today)
        assertThat(done.currentWeeks).isEqualTo(4)
    }

    @Test
    fun `a missed week is forgiven once`() {
        val dates = week(4, 3) + week(3, 3) + week(1, 3)
        assertThat(trainingStreak(dates, 3, today).currentWeeks).isEqualTo(3)

        // Two misses within the grace period break it.
        val twoMisses = week(6, 3) + week(5, 3) + week(3, 3) + week(1, 3)
        assertThat(trainingStreak(twoMisses, 3, today).currentWeeks).isEqualTo(2)
    }

    @Test
    fun `best streak survives a break`() {
        val dates = week(10, 2) + week(9, 2) + week(8, 2) + week(7, 2) + week(1, 2)
        val streak = trainingStreak(dates, weeklyTarget = 2, today = today)
        assertThat(streak.currentWeeks).isEqualTo(1)
        assertThat(streak.bestWeeks).isEqualTo(4)
        assertThat(streak.lastWorkoutDate).isEqualTo(weeksAgo(1, 1))
    }

    @Test
    fun `no workouts means no streak`() {
        val streak = trainingStreak(emptyList(), 3, today)
        assertThat(streak.currentWeeks).isEqualTo(0)
        assertThat(streak.bestWeeks).isEqualTo(0)
    }

    @Test
    fun `moving average trails over seven days`() {
        val d = LocalDate.of(2026, 1, 1)
        val avg = movingAverage(listOf(d to 80.0, d.plusDays(1) to 82.0, d.plusDays(10) to 90.0))
        assertThat(avg.map { it.second }).containsExactly(80.0, 81.0, 90.0).inOrder()
    }
}
