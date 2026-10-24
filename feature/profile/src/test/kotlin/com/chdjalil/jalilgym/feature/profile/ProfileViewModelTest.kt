package com.chdjalil.jalilgym.feature.profile

import app.cash.turbine.test
import com.chdjalil.jalilgym.core.domain.repository.ImportResult
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.testing.MainDispatcherRule
import com.chdjalil.jalilgym.core.testing.repository.FakeBackupRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserPreferencesRepository()
    private val backup = FakeBackupRepository()

    @Test
    fun `emits stored preferences`() = runTest {
        val viewModel = ProfileViewModel(repository, backup)
        viewModel.uiState.test {
            val first = awaitItem()
            val ready = if (first is ProfileUiState.Loading) awaitItem() else first
            assertThat(ready).isEqualTo(ProfileUiState.Ready(UserPreferences()))
        }
    }

    @Test
    fun `events update preferences`() = runTest {
        // Created inside the test so viewModelScope picks up the test Main dispatcher.
        val viewModel = ProfileViewModel(repository, backup)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onEvent(ProfileEvent.ThemeSelected(ThemeMode.LIGHT))
        viewModel.onEvent(ProfileEvent.UnitsSelected(UnitSystem.IMPERIAL))
        viewModel.onEvent(ProfileEvent.EffortScaleSelected(EffortScale.RIR))

        val expected = UserPreferences(
            themeMode = ThemeMode.LIGHT,
            units = UnitSystem.IMPERIAL,
            effortScale = EffortScale.RIR,
        )
        assertThat(repository.current).isEqualTo(expected)
        assertThat(viewModel.uiState.value).isEqualTo(ProfileUiState.Ready(expected))
    }

    @Test
    fun `backups report their outcome`() = runTest {
        val viewModel = ProfileViewModel(repository, backup)
        val messages = mutableListOf<BackupMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect { messages += it } }

        viewModel.exportTo("content://out")
        backup.exportFailure = IOException("revoked")
        viewModel.exportTo("content://denied")
        viewModel.importFrom("content://in")
        backup.importResult = ImportResult.TooNew
        viewModel.importFrom("content://new")
        backup.importResult = ImportResult.Invalid
        viewModel.importFrom("content://junk")

        assertThat(messages).containsExactly(
            BackupMessage.Exported(42),
            BackupMessage.ExportFailed,
            BackupMessage.Imported(42),
            BackupMessage.ImportTooNew,
            BackupMessage.ImportInvalid,
        ).inOrder()
        assertThat(backup.exportedTo).containsExactly("content://out")
        assertThat(viewModel.backupBusy.value).isFalse()
    }
}
