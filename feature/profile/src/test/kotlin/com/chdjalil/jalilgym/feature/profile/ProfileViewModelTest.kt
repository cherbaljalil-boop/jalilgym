package com.chdjalil.jalilgym.feature.profile

import app.cash.turbine.test
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserPreferencesRepository()
    private val viewModel = ProfileViewModel(repository)

    @Test
    fun `emits stored preferences`() = runTest {
        viewModel.uiState.test {
            val first = awaitItem()
            val ready = if (first is ProfileUiState.Loading) awaitItem() else first
            assertThat(ready).isEqualTo(ProfileUiState.Ready(UserPreferences()))
        }
    }

    @Test
    fun `events update preferences`() = runTest {
        viewModel.uiState.test {
            viewModel.onEvent(ProfileEvent.ThemeSelected(ThemeMode.LIGHT))
            viewModel.onEvent(ProfileEvent.UnitsSelected(UnitSystem.IMPERIAL))
            viewModel.onEvent(ProfileEvent.EffortScaleSelected(EffortScale.RIR))

            assertThat(expectMostRecentItem()).isEqualTo(
                ProfileUiState.Ready(
                    UserPreferences(
                        themeMode = ThemeMode.LIGHT,
                        units = UnitSystem.IMPERIAL,
                        effortScale = EffortScale.RIR,
                    ),
                ),
            )
        }
    }
}
