package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.BodyProfile
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class OfflineFirstUserPreferencesRepository @Inject constructor(
    private val dataSource: JgPreferencesDataSource,
) : UserPreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun setThemeMode(themeMode: ThemeMode) = dataSource.setThemeMode(themeMode)

    override suspend fun setUnitSystem(units: UnitSystem) = dataSource.setUnitSystem(units)

    override suspend fun setEffortScale(effortScale: EffortScale) = dataSource.setEffortScale(effortScale)

    override suspend fun setBodyProfile(profile: BodyProfile) = dataSource.setBodyProfile(profile)

    override suspend fun setHalalFilter(filter: HalalFilter) = dataSource.setHalalFilter(filter)

    override suspend fun markStarterProgramApplied() = dataSource.markStarterProgramApplied()

    override suspend fun completeOnboarding(
        goal: Goal?,
        experience: ExperienceLevel?,
        starterProgram: StarterProgram?,
    ) = dataSource.completeOnboarding(goal, experience, starterProgram)
}
