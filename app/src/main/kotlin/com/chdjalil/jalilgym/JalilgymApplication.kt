package com.chdjalil.jalilgym

import android.app.Application
import android.util.Log
import com.chdjalil.jalilgym.core.common.ApplicationScope
import com.chdjalil.jalilgym.core.domain.program.ApplyStarterProgramUseCase
import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.domain.repository.SeedRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
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
    }
}
