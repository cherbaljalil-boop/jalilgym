package com.chdjalil.jalilgym.core.datastore

import androidx.datastore.core.DataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class JgPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<UserPreferencesData>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { it.toModel() }

    suspend fun installedSeedVersion(): Int = dataStore.data.first().installedSeedVersion

    suspend fun setInstalledSeedVersion(version: Int) = update { it.copy(installedSeedVersion = version) }

    suspend fun installedProgramSeedVersion(): Int = dataStore.data.first().installedProgramSeedVersion

    suspend fun setInstalledProgramSeedVersion(version: Int) =
        update { it.copy(installedProgramSeedVersion = version) }

    suspend fun installedFoodSeedVersion(): Int = dataStore.data.first().installedFoodSeedVersion

    suspend fun setInstalledFoodSeedVersion(version: Int) = update { it.copy(installedFoodSeedVersion = version) }

    suspend fun setBodyProfile(profile: BodyProfile) = update { it.copy(bodyProfile = profile.toData()) }

    suspend fun setHalalFilter(filter: HalalFilter) = update { it.copy(halalFilter = filter) }

    suspend fun markStarterProgramApplied() = update { it.copy(starterProgramApplied = true) }

    suspend fun setThemeMode(themeMode: ThemeMode) = update { it.copy(themeMode = themeMode) }

    suspend fun setUnitSystem(units: UnitSystem) = update { it.copy(units = units) }

    suspend fun setEffortScale(effortScale: EffortScale) = update { it.copy(effortScale = effortScale) }

    suspend fun completeOnboarding(goal: Goal?, experience: ExperienceLevel?, starterProgram: StarterProgram?) =
        update {
            it.copy(
                onboardingComplete = true,
                goal = goal,
                experience = experience,
                starterProgram = starterProgram,
            )
        }

    private suspend fun update(transform: (UserPreferencesData) -> UserPreferencesData) {
        dataStore.updateData(transform)
    }
}
