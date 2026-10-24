package com.chdjalil.jalilgym.core.datastore

import androidx.datastore.core.DataStore
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
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class JgPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<UserPreferencesData>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { it.toModel() }

    /** All settings as JSON, for backups. */
    suspend fun exportJson(): JsonElement = backupJson.encodeToJsonElement(UserPreferencesData.serializer(), dataStore.data.first())

    /** Replaces all settings with ones from a backup; unknown fields are ignored. */
    suspend fun restoreJson(element: JsonElement) {
        val restored = backupJson.decodeFromJsonElement(UserPreferencesData.serializer(), element)
        dataStore.updateData { restored }
    }

    suspend fun installedSeedVersion(): Int = dataStore.data.first().installedSeedVersion

    suspend fun setInstalledSeedVersion(version: Int) = update { it.copy(installedSeedVersion = version) }

    suspend fun installedProgramSeedVersion(): Int = dataStore.data.first().installedProgramSeedVersion

    suspend fun setInstalledProgramSeedVersion(version: Int) =
        update { it.copy(installedProgramSeedVersion = version) }

    suspend fun installedFoodSeedVersion(): Int = dataStore.data.first().installedFoodSeedVersion

    suspend fun setInstalledFoodSeedVersion(version: Int) = update { it.copy(installedFoodSeedVersion = version) }

    suspend fun installedRecipeSeedVersion(): Int = dataStore.data.first().installedRecipeSeedVersion

    suspend fun setInstalledRecipeSeedVersion(version: Int) = update { it.copy(installedRecipeSeedVersion = version) }

    suspend fun setBodyProfile(profile: BodyProfile) = update { it.copy(bodyProfile = profile.toData()) }

    suspend fun setHalalFilter(filter: HalalFilter) = update { it.copy(halalFilter = filter) }

    suspend fun setLastCheckIn(date: LocalDate) = update { it.copy(lastCheckIn = date.toString()) }

    suspend fun setHealthConnectEnabled(enabled: Boolean) = update { it.copy(healthConnectEnabled = enabled) }

    suspend fun setWaterGoal(ml: Int?) = update { it.copy(waterGoalMl = ml) }

    suspend fun setReminders(settings: ReminderSettings) = update { it.copy(reminders = settings.toData()) }

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

private val backupJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
