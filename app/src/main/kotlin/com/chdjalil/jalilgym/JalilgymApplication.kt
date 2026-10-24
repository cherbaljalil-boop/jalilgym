package com.chdjalil.jalilgym

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.chdjalil.jalilgym.core.common.ApplicationScope
import com.chdjalil.jalilgym.core.domain.program.ApplyStarterProgramUseCase
import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.domain.repository.CloudSyncRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.SeedRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.notifications.ReminderScheduler
import com.chdjalil.jalilgym.feature.widgets.TodayWidget
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@HiltAndroidApp
class JalilgymApplication : Application() {

    @Inject
    lateinit var seedRepository: SeedRepository

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var applyStarterProgram: ApplyStarterProgramUseCase

    @Inject
    lateinit var achievementRepository: AchievementRepository

    @Inject
    lateinit var healthRepository: HealthRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var cloudSyncRepository: CloudSyncRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /** Asks for Health Connect and cloud syncs; extra requests while one is pending are merged. */
    private val backgroundSyncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Sync when access is granted and whenever the app goes to the background,
            // so other health apps see today's workouts, weigh-ins and meals.
            runCatching { healthRepository.refresh() }
            val becameReady = healthRepository.observeStatus()
                .map { it.ready }
                .distinctUntilChanged()
                .filter { it }
                .map { }
            merge(becameReady, backgroundSyncRequests)
                .debounce(BACKGROUND_SYNC_DEBOUNCE_MS)
                .collect {
                    runCatching { healthRepository.sync() }
                        .onFailure { Log.e(TAG, "Health Connect sync failed", it) }
                }
        }
        applicationScope.launch {
            // Rebook on every start too, so a time zone change moves reminders to local time.
            preferencesRepository.userPreferences
                .map { it.reminders }
                .distinctUntilChanged()
                .collect { settings ->
                    runCatching { reminderScheduler.apply(settings) }
                        .onFailure { Log.e(TAG, "Couldn't schedule reminders", it) }
                }
        }
        applicationScope.launch {
            runCatching { TodayWidget.refreshOnChanges(this@JalilgymApplication) }
                .onFailure { Log.e(TAG, "Widget updates stopped", it) }
        }
        applicationScope.launch {
            // Optional cloud sync: catch up on start,
            // then upload whenever the app goes to the background.
            runCatching {
                cloudSyncRepository.initialize()
                cloudSyncRepository.sync()
            }.onFailure { Log.e(TAG, "Cloud sync failed", it) }
            backgroundSyncRequests
                .debounce(BACKGROUND_SYNC_DEBOUNCE_MS)
                .collect {
                    runCatching { cloudSyncRepository.sync() }
                        .onFailure { Log.e(TAG, "Cloud sync failed", it) }
                }
        }
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityStopped(activity: Activity) {
                    if (activity is MainActivity) backgroundSyncRequests.tryEmit(Unit)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
        applicationScope.launch {
            // Badges earned outside a workout (a photo, a custom exercise…) unlock as soon as they're earned.
            achievementRepository.observeAchievements()
                .map { list -> list.any { !it.isUnlocked && it.progress >= it.target } }
                .filter { it }
                .collect {
                    runCatching { achievementRepository.syncUnlocks() }
                        .onFailure { Log.e(TAG, "Couldn't sync achievements", it) }
                }
        }
        applicationScope.launch {
            // Screens observe Room, so they update as soon as the seed lands.
            runCatching { seedRepository.seedIfNeeded() }
                .onFailure { Log.e(TAG, "Seed import failed", it) }

            // Once onboarding picks a starter program (now or on an earlier launch), set it up.
            preferencesRepository.userPreferences
                .map { it.onboardingComplete && it.starterProgram != null && !it.starterProgramApplied }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    runCatching { applyStarterProgram(getString(R.string.default_custom_program_name)) }
                        .onFailure { Log.e(TAG, "Couldn't set up the starter program", it) }
                }
        }
    }

    private companion object {
        const val TAG = "Jalilgym"
        const val BACKGROUND_SYNC_DEBOUNCE_MS = 2_000L
    }
}
