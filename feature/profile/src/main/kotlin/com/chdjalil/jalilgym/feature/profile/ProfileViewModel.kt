package com.chdjalil.jalilgym.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.BackupRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthStatus
import com.chdjalil.jalilgym.core.domain.repository.ImportResult
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Ready(val preferences: UserPreferences) : ProfileUiState
}

sealed interface ProfileEvent {
    data class ThemeSelected(val themeMode: ThemeMode) : ProfileEvent

    data class UnitsSelected(val units: UnitSystem) : ProfileEvent

    data class EffortScaleSelected(val effortScale: EffortScale) : ProfileEvent
}

/** Outcomes of export and import, shown once. */
sealed interface BackupMessage {
    data class Exported(val rows: Int) : BackupMessage

    data object ExportFailed : BackupMessage

    data class Imported(val rows: Int) : BackupMessage

    data object ImportTooNew : BackupMessage

    data object ImportInvalid : BackupMessage
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    private val healthRepository: HealthRepository,
) : ViewModel() {

    val healthStatus: StateFlow<HealthStatus> = healthRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthStatus())

    val healthPermissions: Set<String> get() = healthRepository.requiredPermissions

    /** Re-checks Health Connect (call when the screen shows: the user may have changed access elsewhere). */
    fun refreshHealth() {
        viewModelScope.launch { healthRepository.refresh() }
    }

    fun setHealthEnabled(enabled: Boolean) {
        viewModelScope.launch {
            healthRepository.setEnabled(enabled)
            healthRepository.refresh()
            if (enabled) healthRepository.sync()
        }
    }

    /** After Health Connect's permission screen. */
    fun onHealthPermissionsResult() {
        viewModelScope.launch {
            healthRepository.refresh()
            healthRepository.sync()
        }
    }

    fun syncHealthNow() {
        viewModelScope.launch { healthRepository.sync() }
    }

    private val _backupBusy = MutableStateFlow(false)

    /** True while a backup is being written or restored. */
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    private val _messages = MutableSharedFlow<BackupMessage>(extraBufferCapacity = 2)
    val messages: SharedFlow<BackupMessage> = _messages.asSharedFlow()

    fun exportTo(uri: String) = runBackup {
        try {
            BackupMessage.Exported(backupRepository.export(uri))
        } catch (e: IOException) {
            BackupMessage.ExportFailed
        } catch (e: SecurityException) {
            BackupMessage.ExportFailed
        }
    }

    fun importFrom(uri: String) = runBackup {
        when (
            val result = try {
                backupRepository.import(uri)
            } catch (e: IOException) {
                ImportResult.Invalid
            } catch (e: SecurityException) {
                ImportResult.Invalid
            }
        ) {
            is ImportResult.Success -> BackupMessage.Imported(result.rows)
            ImportResult.TooNew -> BackupMessage.ImportTooNew
            ImportResult.Invalid -> BackupMessage.ImportInvalid
        }
    }

    private fun runBackup(block: suspend () -> BackupMessage) {
        if (_backupBusy.value) return
        _backupBusy.value = true
        viewModelScope.launch {
            try {
                _messages.emit(block())
            } finally {
                _backupBusy.value = false
            }
        }
    }

    val uiState: StateFlow<ProfileUiState> = preferencesRepository.userPreferences
        .map<UserPreferences, ProfileUiState> { ProfileUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            when (event) {
                is ProfileEvent.ThemeSelected -> preferencesRepository.setThemeMode(event.themeMode)
                is ProfileEvent.UnitsSelected -> preferencesRepository.setUnitSystem(event.units)
                is ProfileEvent.EffortScaleSelected -> preferencesRepository.setEffortScale(event.effortScale)
            }
        }
    }
}
