package com.chdjalil.jalilgym

import app.cash.turbine.test
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `defaults to dark theme and follows changes`() = runTest {
        val viewModel = MainActivityViewModel(preferences)

        viewModel.uiState.test {
            assertThat(expectMostRecentItem()).isEqualTo(MainActivityUiState.Success(ThemeMode.DARK))

            preferences.setThemeMode(ThemeMode.LIGHT)

            assertThat(awaitItem()).isEqualTo(MainActivityUiState.Success(ThemeMode.LIGHT))
        }
    }
}
