package com.chdjalil.jalilgym.core.testing.repository

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
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)

    override val userPreferences: Flow<UserPreferences> = state

    val current: UserPreferences get() = state.value

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setUnitSystem(units: UnitSystem) = state.update { it.copy(units = units) }

    override suspend fun setEffortScale(effortScale: EffortScale) =
        state.update { it.copy(effortScale = effortScale) }

    override suspend fun setBodyProfile(profile: BodyProfile) = state.update { it.copy(bodyProfile = profile) }

    override suspend fun setHalalFilter(filter: HalalFilter) = state.update { it.copy(halalFilter = filter) }

    override suspend fun setLastCheckIn(date: LocalDate) = state.update { it.copy(lastCheckIn = date) }

    override suspend fun setHealthConnectEnabled(enabled: Boolean) = state.update { it.copy(healthConnectEnabled = enabled) }

    override suspend fun setWaterGoal(ml: Int?) = state.update { it.copy(waterGoalMl = ml) }

    override suspend fun markStarterProgramApplied() = state.update { it.copy(starterProgramApplied = true) }

    override suspend fun completeOnboarding(
        goal: Goal?,
        experience: ExperienceLevel?,
        starterProgram: StarterProgram?,
    ) = state.update {
        it.copy(onboardingComplete = true, goal = goal, experience = experience, starterProgram = starterProgram)
    }
}
