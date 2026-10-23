package com.chdjalil.jalilgym.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.EffortScale
import com.chdjalil.jalilgym.core.model.ThemeMode
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Ready(val preferences: UserPreferences) : ProfileUiState
}

sealed interface ProfileEvent {
    data class ThemeSelected(val themeMode: ThemeMode) : ProfileEvent

    data class UnitsSelected(val units: UnitSystem) : ProfileEvent

    data class EffortScaleSelected(val effortScale: EffortScale) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

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
