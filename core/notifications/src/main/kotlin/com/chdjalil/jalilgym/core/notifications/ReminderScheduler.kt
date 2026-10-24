package com.chdjalil.jalilgym.core.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chdjalil.jalilgym.core.domain.reminders.nextReminderAt
import com.chdjalil.jalilgym.core.domain.reminders.nextWeeklySummaryAt
import com.chdjalil.jalilgym.core.model.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one pending job per notification, each due at its next time. A job
 * that fires books the following one itself, so nothing runs in between.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Call whenever the settings change (and on app start, to pick up time zone changes). */
    fun apply(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) {
        ReminderNotifications.createChannels(context)
        scheduleWorkout(settings, now, ExistingWorkPolicy.REPLACE)
        scheduleSummary(settings, now, ExistingWorkPolicy.REPLACE)
    }

    /** From inside a running job: queue the next one behind it rather than cancelling it. */
    internal fun scheduleNextWorkout(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) =
        scheduleWorkout(settings, now, ExistingWorkPolicy.APPEND_OR_REPLACE)

    internal fun scheduleNextSummary(settings: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()) =
        scheduleSummary(settings, now, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun scheduleWorkout(settings: ReminderSettings, now: ZonedDateTime, policy: ExistingWorkPolicy) {
        val next = nextReminderAt(now, settings.workoutDays, settings.workoutTime)
        if (next == null) {
            workManager.cancelUniqueWork(WORKOUT_WORK)
        } else {
            workManager.enqueueUniqueWork(WORKOUT_WORK, policy, request<WorkoutReminderWorker>(now, next))
        }
    }

    private fun scheduleSummary(settings: ReminderSettings, now: ZonedDateTime, policy: ExistingWorkPolicy) {
        if (!settings.weeklySummary) {
            workManager.cancelUniqueWork(SUMMARY_WORK)
        } else {
            workManager.enqueueUniqueWork(SUMMARY_WORK, policy, request<WeeklySummaryWorker>(now, nextWeeklySummaryAt(now)))
        }
    }

    private inline fun <reified W : androidx.work.ListenableWorker> request(now: ZonedDateTime, at: ZonedDateTime): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<W>()
            .setInitialDelay(Duration.between(now, at).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()

    internal companion object {
        const val WORKOUT_WORK = "jalilgym-workout-reminder"
        const val SUMMARY_WORK = "jalilgym-weekly-summary"
    }
}
