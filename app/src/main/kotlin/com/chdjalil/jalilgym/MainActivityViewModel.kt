package com.chdjalil.jalilgym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val themeMode: ThemeMode, val onboardingComplete: Boolean) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = preferencesRepository.userPreferences
        .map<_, MainActivityUiState> { MainActivityUiState.Success(it.themeMode, it.onboardingComplete) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainActivityUiState.Loading)
}
