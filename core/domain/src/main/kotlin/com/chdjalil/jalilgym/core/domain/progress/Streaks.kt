package com.chdjalil.jalilgym.core.domain.progress

import com.chdjalil.jalilgym.core.model.TrainingStreak
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Weeks start on Monday. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** One missed week is forgiven per this many counted weeks (docs/BLUEPRINT.md §3.6). */
const val GRACE_EVERY_WEEKS = 8

/**
 * Training streak in weeks on target. Rest days never break it: a week only
 * needs [weeklyTarget] workouts. The current week counts once it's on target
 * and never breaks the streak while it's still in progress. One missed week is
 * forgiven per [GRACE_EVERY_WEEKS] counted weeks (a grace week doesn't add to
 * the count, and a streak can't start with one).
 */
fun trainingStreak(workoutDates: List<LocalDate>, weeklyTarget: Int, today: LocalDate): TrainingStreak {
    val target = weeklyTarget.coerceAtLeast(1)
    val perWeek = workoutDates.groupingBy { it.weekStart() }.eachCount()
    val thisWeek = today.weekStart()
    val thisWeekCount = perWeek[thisWeek] ?: 0

    fun onTarget(week: LocalDate) = (perWeek[week] ?: 0) >= target

    // Current streak: walk back from last week.
    var current = 0
    var cooldown = 0 // counted weeks needed before another grace
    var graceUsed = false
    var week = thisWeek.minusWeeks(1)
    val earliest = perWeek.keys.minOrNull()
    while (earliest != null && !week.isBefore(earliest)) {
        if (onTarget(week)) {
            current++
            if (cooldown > 0) cooldown--
        } else if (current > 0 && (!graceUsed || cooldown == 0) && onTarget(week.minusWeeks(1))) {
            // Forgive this week only if the streak continues before it.
            graceUsed = true
            cooldown = GRACE_EVERY_WEEKS
        } else {
            break
        }
        week = week.minusWeeks(1)
    }
    if (onTarget(thisWeek)) current++

    // Best streak: same rules walking forward through all weeks.
    var best = 0
    if (earliest != null) {
        var run = 0
        var runCooldown = 0
        var runGraceUsed = false
        var w: LocalDate = earliest
        while (!w.isAfter(thisWeek)) {
            when {
                onTarget(w) -> {
                    run++
                    if (runCooldown > 0) runCooldown--
                }
                w == thisWeek -> Unit // In progress: neither counts nor breaks.
                run > 0 && (!runGraceUsed || runCooldown == 0) && onTarget(w.plusWeeks(1)) -> {
                    runGraceUsed = true
                    runCooldown = GRACE_EVERY_WEEKS
                }
                else -> {
                    run = 0
                    runGraceUsed = false
                    runCooldown = 0
                }
            }
            best = maxOf(best, run)
            w = w.plusWeeks(1)
        }
    }
    return TrainingStreak(
        currentWeeks = current,
        bestWeeks = maxOf(best, current),
        thisWeekWorkouts = thisWeekCount,
        weeklyTarget = target,
        lastWorkoutDate = workoutDates.maxOrNull(),
    )
}

/** Trailing moving average over the last [days] calendar days for each entry (for the weight trend). */
fun movingAverage(points: List<Pair<LocalDate, Double>>, days: Int = 7): List<Pair<LocalDate, Double>> {
    val sorted = points.sortedBy { it.first }
    return sorted.map { (date, _) ->
        val window = sorted.filter { (d, _) -> !d.isAfter(date) && d.isAfter(date.minusDays(days.toLong())) }
        date to window.map { it.second }.average()
    }
}
