package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
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

    override suspend fun completeOnboarding(goal: Goal, experience: ExperienceLevel) =
        state.update { it.copy(onboardingComplete = true, goal = goal, experience = experience) }
}
