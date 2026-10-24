package com.chdjalil.jalilgym.core.domain.reminders

import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** The weekly summary goes out on Sunday evening. */
val WEEKLY_SUMMARY_DAY: DayOfWeek = DayOfWeek.SUNDAY
val WEEKLY_SUMMARY_TIME: LocalTime = LocalTime.of(19, 0)

/** The next time strictly after [now] that falls on one of [days] at [time]; null with no days. */
fun nextReminderAt(now: ZonedDateTime, days: Set<DayOfWeek>, time: LocalTime): ZonedDateTime? {
    if (days.isEmpty()) return null
    return (0..7).asSequence()
        .map { now.toLocalDate().plusDays(it.toLong()) }
        .filter { it.dayOfWeek in days }
        // atZone moves a time that falls in a DST gap forward, so it always exists.
        .map { it.atTime(time).atZone(now.zone) }
        .first { it.isAfter(now) }
}

fun nextWeeklySummaryAt(now: ZonedDateTime): ZonedDateTime = nextReminderAt(now, setOf(WEEKLY_SUMMARY_DAY), WEEKLY_SUMMARY_TIME)!!

/** No nudge when the user already trained today or has a workout open. */
fun shouldRemindToTrain(history: List<WorkoutHistoryItem>, workoutInProgress: Boolean, today: LocalDate, zone: ZoneId): Boolean =
    !workoutInProgress && history.none { Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() == today }

data class WeeklySummary(
    val workouts: Int,
    val weeklyTarget: Int,
    val volumeKg: Double,
    /** Days this week with anything in the food diary. */
    val daysLogged: Int,
    /** This week's average weight minus last week's; null without weigh-ins in both. */
    val weightChangeKg: Double?,
) {
    val isEmpty: Boolean get() = workouts == 0 && daysLogged == 0 && weightChangeKg == null
}

/** The Monday-to-Sunday week that contains [date]. */
fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weeklySummary(
    today: LocalDate,
    zone: ZoneId,
    history: List<WorkoutHistoryItem>,
    weeklyTarget: Int,
    loggedDays: Set<LocalDate>,
    weights: List<Pair<LocalDate, Double>>,
): WeeklySummary {
    val start = weekStartOf(today)
    val week = start..start.plusDays(6)
    val workouts = history.filter { Instant.ofEpochMilli(it.endedAt).atZone(zone).toLocalDate() in week }
    fun averageIn(range: ClosedRange<LocalDate>) = weights.filter { it.first in range }.map { it.second }.takeIf { it.isNotEmpty() }?.average()
    val thisWeek = averageIn(week)
    val lastWeek = averageIn(start.minusDays(7)..start.minusDays(1))
    return WeeklySummary(
        workouts = workouts.size,
        weeklyTarget = weeklyTarget,
        volumeKg = workouts.sumOf { it.totalVolumeKg },
        daysLogged = loggedDays.count { it in week },
        weightChangeKg = if (thisWeek != null && lastWeek != null) thisWeek - lastWeek else null,
    )
}
