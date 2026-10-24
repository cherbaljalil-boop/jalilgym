package com.chdjalil.jalilgym.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chdjalil.jalilgym.core.domain.reminders.shouldRemindToTrain
import com.chdjalil.jalilgym.core.domain.reminders.weekStartOf
import com.chdjalil.jalilgym.core.domain.reminders.weeklySummary
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/** Workers are created by WorkManager, so they reach the app's singletons through this. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderEntryPoint {
    fun preferences(): UserPreferencesRepository

    fun workouts(): WorkoutRepository

    fun programs(): ProgramRepository

    fun progress(): ProgressRepository

    fun nutrition(): NutritionRepository

    fun body(): BodyRepository

    fun scheduler(): ReminderScheduler
}

private fun Context.entryPoint() = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)

internal class WorkoutReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = applicationContext.entryPoint()
        val settings = deps.preferences().userPreferences.first().reminders
        if (settings.workoutDays.isEmpty()) return Result.success()
        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            // A reminder delayed past midnight (Doze) is for a day that's gone.
            if (today.dayOfWeek in settings.workoutDays) {
                val inProgress = deps.workouts().observeActiveSession().first() != null
                if (shouldRemindToTrain(deps.workouts().observeHistory().first(), inProgress, today, zone)) {
                    val active = deps.programs().observeActiveProgram().first()
                    ReminderNotifications.showWorkoutReminder(applicationContext, active?.nextDay?.name, active?.program?.name)
                }
            }
        } finally {
            deps.scheduler().scheduleNextWorkout(settings)
        }
        return Result.success()
    }
}

internal class WeeklySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = applicationContext.entryPoint()
        val preferences = deps.preferences().userPreferences.first()
        if (!preferences.reminders.weeklySummary) return Result.success()
        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = weekStartOf(today)
            val loggedDays = (0L..6L).map { start.plusDays(it) }
                .filter { deps.nutrition().observeDiary(it).first().entries.isNotEmpty() }
                .toSet()
            val summary = weeklySummary(
                today = today,
                zone = zone,
                history = deps.workouts().observeHistory().first(),
                weeklyTarget = deps.progress().observeTrainingStreak().first().weeklyTarget,
                loggedDays = loggedDays,
                weights = deps.body().observeWeights().first().map { it.date to it.weightKg },
            )
            if (!summary.isEmpty) ReminderNotifications.showWeeklySummary(applicationContext, summary, preferences.units)
        } finally {
            deps.scheduler().scheduleNextSummary(preferences.reminders)
        }
        return Result.success()
    }
}
