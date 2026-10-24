package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.HalalFilter
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

    /** Any answer may be null when the user skipped it. */
    suspend fun markStarterProgramApplied()

    suspend fun completeOnboarding(goal: Goal?, experience: ExperienceLevel?, starterProgram: StarterProgram?)
}
