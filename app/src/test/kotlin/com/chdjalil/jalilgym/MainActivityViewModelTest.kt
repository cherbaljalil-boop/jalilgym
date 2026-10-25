package com.chdjalil.jalilgym

import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `defaults to dark theme with onboarding pending, then follows changes`() = runTest {
        val viewModel = MainActivityViewModel(preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value)
            .isEqualTo(MainActivityUiState.Success(ThemeMode.DARK, onboardingComplete = false))

        preferences.setThemeMode(ThemeMode.LIGHT)
        preferences.completeOnboarding(goal = null, experience = null, starterProgram = null)

        assertThat(viewModel.uiState.value)
            .isEqualTo(MainActivityUiState.Success(ThemeMode.LIGHT, onboardingComplete = true))
    }
}
