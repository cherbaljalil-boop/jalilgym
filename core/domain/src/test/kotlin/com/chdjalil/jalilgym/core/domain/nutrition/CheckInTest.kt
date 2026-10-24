package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.DietGoal
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class CheckInTest {

    private val today = LocalDate.of(2026, 10, 24)

    /** Two weigh-ins in each week: [previous] then [recent] kg. */
    private fun weeks(previous: Double, recent: Double) = listOf(
        today.minusDays(12) to previous,
        today.minusDays(9) to previous,
        today.minusDays(4) to recent,
        today to recent,
    )

    @Test
    fun needsTwoWeighInsInBothWeeks() {
        val sparse = listOf(today.minusDays(10) to 80.0, today to 79.0)
        assertThat(weeklyCheckIn(sparse, DietGoal.CUT, today).verdict).isEqualTo(CheckInVerdict.NOT_ENOUGH_DATA)
        // Weigh-ins older than two weeks don't count.
        val old = listOf(today.minusDays(20) to 81.0, today.minusDays(15) to 80.5) + weeks(80.0, 79.5).takeLast(2)
        assertThat(weeklyCheckIn(old, DietGoal.CUT, today).verdict).isEqualTo(CheckInVerdict.NOT_ENOUGH_DATA)
    }

    @Test
    fun cutOnTrackStalledAndTooFast() {
        val onTrack = weeklyCheckIn(weeks(80.0, 79.5), DietGoal.CUT, today)
        assertThat(onTrack.verdict).isEqualTo(CheckInVerdict.ON_TRACK)
        assertThat(onTrack.weeklyChangeKg).isWithin(1e-9).of(-0.5)
        assertThat(onTrack.adjustmentKcal).isEqualTo(0)

        val stalled = weeklyCheckIn(weeks(80.0, 79.95), DietGoal.CUT, today)
        assertThat(stalled.verdict).isEqualTo(CheckInVerdict.TOO_SLOW)
        assertThat(stalled.adjustmentKcal).isEqualTo(-100)

        val crash = weeklyCheckIn(weeks(80.0, 78.9), DietGoal.CUT, today)
        assertThat(crash.verdict).isEqualTo(CheckInVerdict.TOO_FAST)
        assertThat(crash.adjustmentKcal).isEqualTo(100)
    }

    @Test
    fun bulkAndMaintenance() {
        assertThat(weeklyCheckIn(weeks(70.0, 70.0), DietGoal.LEAN_BULK, today).adjustmentKcal).isEqualTo(100)
        assertThat(weeklyCheckIn(weeks(70.0, 70.2), DietGoal.LEAN_BULK, today).verdict).isEqualTo(CheckInVerdict.ON_TRACK)
        assertThat(weeklyCheckIn(weeks(70.0, 70.6), DietGoal.LEAN_BULK, today).adjustmentKcal).isEqualTo(-100)
        assertThat(weeklyCheckIn(weeks(70.0, 70.1), DietGoal.MAINTAIN, today).verdict).isEqualTo(CheckInVerdict.ON_TRACK)
        assertThat(weeklyCheckIn(weeks(70.0, 70.4), DietGoal.MAINTAIN, today).adjustmentKcal).isEqualTo(-100)
        assertThat(weeklyCheckIn(weeks(70.0, 69.6), DietGoal.MAINTAIN, today).adjustmentKcal).isEqualTo(100)
    }

    @Test
    fun dueWeekly() {
        assertThat(checkInDue(null, today)).isTrue()
        assertThat(checkInDue(today.minusDays(6), today)).isFalse()
        assertThat(checkInDue(today.minusDays(7), today)).isTrue()
    }

    @Test
    fun adjustingMovesCarbsAndRespectsTheFloor() {
        val targets = MacroTargets(2000, 150, 200, 70)
        assertThat(adjustTargets(targets, -100, 1500)).isEqualTo(MacroTargets(1900, 150, 175, 70))
        assertThat(adjustTargets(targets, 100, 1500)).isEqualTo(MacroTargets(2100, 150, 225, 70))
        assertThat(adjustTargets(MacroTargets(1250, 120, 50, 40), -100, 1200)).isEqualTo(MacroTargets(1200, 120, 38, 40))
    }
}
