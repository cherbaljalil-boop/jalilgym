package com.chdjalil.jalilgym.core.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.chdjalil.jalilgym.core.domain.reminders.WeeklySummary
import com.chdjalil.jalilgym.core.model.ReminderSettings
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReminderSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var scheduler: ReminderScheduler

    // Wednesday 2026-10-21, 09:00.
    private val now = LocalDateTime.parse("2026-10-21T09:00").atZone(ZoneId.of("Europe/Paris"))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = ReminderScheduler(context)
    }

    private fun pending(name: String): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get().filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun `books the next reminder and the Sunday summary`() {
        scheduler.apply(ReminderSettings(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), LocalTime.of(18, 30), weeklySummary = true), now)

        val workout = pending(ReminderScheduler.WORKOUT_WORK).single()
        assertThat(workout.initialDelayMillis).isEqualTo(TimeUnit.MINUTES.toMillis(9 * 60 + 30))
        val summary = pending(ReminderScheduler.SUMMARY_WORK).single()
        // Sunday 19:00 is 4 days and 10 hours away on the clock, plus the hour the clocks go back on the 25th.
        assertThat(summary.initialDelayMillis).isEqualTo(TimeUnit.HOURS.toMillis(4 * 24 + 11))
    }

    @Test
    fun `changing the settings replaces what was booked, and turning off cancels it`() {
        scheduler.apply(ReminderSettings(setOf(DayOfWeek.WEDNESDAY), weeklySummary = true), now)
        scheduler.apply(ReminderSettings(setOf(DayOfWeek.THURSDAY), weeklySummary = true), now)
        assertThat(pending(ReminderScheduler.WORKOUT_WORK).single().initialDelayMillis).isEqualTo(TimeUnit.HOURS.toMillis(24 + 9))

        scheduler.apply(ReminderSettings(), now)
        assertThat(pending(ReminderScheduler.WORKOUT_WORK)).isEmpty()
        assertThat(pending(ReminderScheduler.SUMMARY_WORK)).isEmpty()
    }

    @Test
    fun `summary text covers each part of the week`() {
        val text = ReminderNotifications.summaryText(context, WeeklySummary(3, 4, 12_450.0, 5, -0.4), UnitSystem.METRIC)
        assertThat(text).isEqualTo("3 of 4 workouts · 12,450 kg lifted · food logged 5 of 7 days · weight −0.4 kg")
        val quiet = ReminderNotifications.summaryText(context, WeeklySummary(0, 3, 0.0, 2, null), UnitSystem.IMPERIAL)
        assertThat(quiet).isEqualTo("0 of 3 workouts · food logged 2 of 7 days")
    }
}
