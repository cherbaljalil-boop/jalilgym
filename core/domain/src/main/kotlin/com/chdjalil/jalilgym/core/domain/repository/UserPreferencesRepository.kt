package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.ReminderSettings
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUnitSystem(units: UnitSystem)

    suspend fun setEffortScale(effortScale: EffortScale)

    suspend fun setBodyProfile(profile: BodyProfile)

    suspend fun setHalalFilter(filter: HalalFilter)

    suspend fun setLastCheckIn(date: LocalDate)

    suspend fun setHealthConnectEnabled(enabled: Boolean)

    /** Null goes back to the automatic goal. */
    suspend fun setWaterGoal(ml: Int?)

    suspend fun setReminders(settings: ReminderSettings)

    /** Takes the account-wide part of [settings] (see [syncedPart]) from another phone; device settings stay. */
    suspend fun applySyncedSettings(settings: UserPreferences)

    /** Any answer may be null when the user skipped it. */
    suspend fun markStarterProgramApplied()

    suspend fun completeOnboarding(goal: Goal?, experience: ExperienceLevel?, starterProgram: StarterProgram?)
}

/**
 * The settings that follow an account between phones with cloud sync. Health
 * Connect access and seed bookkeeping belong to each phone and are left at
 * their defaults here.
 */
fun UserPreferences.syncedPart(): UserPreferences = UserPreferences(
    onboardingComplete = onboardingComplete,
    goal = goal,
    experience = experience,
    units = units,
    themeMode = themeMode,
    effortScale = effortScale,
    starterProgram = starterProgram,
    starterProgramApplied = starterProgramApplied,
    bodyProfile = bodyProfile,
    halalFilter = halalFilter,
    lastCheckIn = lastCheckIn,
    waterGoalMl = waterGoalMl,
    reminders = reminders,
)
