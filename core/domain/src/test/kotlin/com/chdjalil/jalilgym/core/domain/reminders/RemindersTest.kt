package com.chdjalil.jalilgym.core.domain.reminders

import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class RemindersTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val six = LocalTime.of(18, 0)

    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    @Test
    fun `next reminder is later today when the time hasn't passed`() {
        // Wednesday 2026-10-21.
        assertThat(nextReminderAt(at("2026-10-21T09:00"), setOf(MONDAY, WEDNESDAY, FRIDAY), six)).isEqualTo(at("2026-10-21T18:00"))
    }

    @Test
    fun `next reminder skips to the next chosen day once today's has passed`() {
        assertThat(nextReminderAt(at("2026-10-21T18:00"), setOf(MONDAY, WEDNESDAY, FRIDAY), six)).isEqualTo(at("2026-10-23T18:00"))
        assertThat(nextReminderAt(at("2026-10-23T20:00"), setOf(MONDAY, WEDNESDAY, FRIDAY), six)).isEqualTo(at("2026-10-26T18:00"))
    }

    @Test
    fun `a single day comes round a week later`() {
        assertThat(nextReminderAt(at("2026-10-21T19:00"), setOf(WEDNESDAY), six)).isEqualTo(at("2026-10-28T18:00"))
    }

    @Test
    fun `no days means no reminder`() {
        assertThat(nextReminderAt(at("2026-10-21T09:00"), emptySet(), six)).isNull()
    }

    @Test
    fun `reminder keeps local time across the clock change`() {
        // Clocks go back on Sunday 2026-10-25 in Paris.
        val next = nextReminderAt(at("2026-10-24T19:00"), setOf(MONDAY), six)!!
        assertThat(next.toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-10-26T18:00"))
        assertThat(nextWeeklySummaryAt(at("2026-10-24T19:00")).toLocalDateTime()).isEqualTo(LocalDateTime.parse("2026-10-25T19:00"))
    }

    private fun workout(endedAt: String, volume: Double = 1_000.0): WorkoutHistoryItem {
        val end = at(endedAt).toInstant().toEpochMilli()
        return WorkoutHistoryItem("w$endedAt", "Push", end - 3_600_000, end, volume, 5, 15)
    }

    @Test
    fun `no nudge after training today or during a workout`() {
        val today = LocalDate.parse("2026-10-21")
        assertThat(shouldRemindToTrain(listOf(workout("2026-10-20T19:00")), false, today, zone)).isTrue()
        assertThat(shouldRemindToTrain(listOf(workout("2026-10-21T07:30")), false, today, zone)).isFalse()
        assertThat(shouldRemindToTrain(emptyList(), true, today, zone)).isFalse()
    }

    @Test
    fun `weekly summary counts this Monday-to-Sunday week`() {
        val sunday = LocalDate.parse("2026-10-25")
        val summary = weeklySummary(
            today = sunday,
            zone = zone,
            history = listOf(workout("2026-10-18T10:00"), workout("2026-10-19T18:00", 2_000.0), workout("2026-10-23T18:00", 3_000.0)),
            weeklyTarget = 3,
            loggedDays = setOf(LocalDate.parse("2026-10-17"), LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-20")),
            weights = listOf(
                LocalDate.parse("2026-10-13") to 81.0,
                LocalDate.parse("2026-10-16") to 81.0,
                LocalDate.parse("2026-10-20") to 80.6,
                LocalDate.parse("2026-10-24") to 80.4,
            ),
        )
        assertThat(summary.workouts).isEqualTo(2)
        assertThat(summary.volumeKg).isEqualTo(5_000.0)
        assertThat(summary.daysLogged).isEqualTo(2)
        assertThat(summary.weightChangeKg!!).isWithin(0.001).of(-0.5)
        assertThat(summary.isEmpty).isFalse()
    }

    @Test
    fun `an empty week is flagged so no summary is sent`() {
        val summary = weeklySummary(LocalDate.parse("2026-10-25"), zone, emptyList(), 3, emptySet(), emptyList())
        assertThat(summary.isEmpty).isTrue()
        assertThat(summary.weightChangeKg).isNull()
    }
}
