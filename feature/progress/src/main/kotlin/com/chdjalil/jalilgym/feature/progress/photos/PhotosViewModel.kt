package com.chdjalil.jalilgym.feature.progress.photos

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.ProgressPhoto
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.progress.navigation.AFTER_ID_ARG
import com.chdjalil.jalilgym.feature.progress.navigation.BEFORE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotosUiState(
    val isLoading: Boolean = true,
    /** Newest first, filtered by [pose] when set. */
    val photos: List<ProgressPhoto> = emptyList(),
    val pose: PhotoPose? = null,
    /** Up to two photos picked for comparison, in tap order. */
    val selected: List<String> = emptyList(),
    val importFailed: Boolean = false,
    val units: UnitSystem = UnitSystem.METRIC,
)

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val bodyRepository: BodyRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val pose = MutableStateFlow<PhotoPose?>(null)
    private val selected = MutableStateFlow<List<String>>(emptyList())
    private val importFailed = MutableStateFlow(false)

    val uiState: StateFlow<PhotosUiState> = combine(
        bodyRepository.observePhotos(),
        pose,
        selected,
        importFailed,
        preferencesRepository.userPreferences,
    ) { photos, pose, selected, failed, prefs ->
        val shown = photos.filter { pose == null || it.pose == pose }
        PhotosUiState(false, shown, pose, selected.filter { id -> photos.any { it.id == id } }, failed, prefs.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotosUiState())

    fun selectPose(value: PhotoPose?) {
        pose.value = value
        selected.value = emptyList()
    }

    /** Tapping toggles a photo; picking a third replaces the oldest pick. */
    fun toggle(id: String) = selected.update { current ->
        when {
            id in current -> current - id
            current.size >= 2 -> current.drop(1) + id
            else -> current + id
        }
    }

    fun clearSelection() {
        selected.value = emptyList()
    }

    fun addPhoto(uri: String, pose: PhotoPose) {
        viewModelScope.launch {
            runCatching { bodyRepository.addPhoto(uri, pose, LocalDate.now()) }
                .onFailure {
                    Log.w(TAG, "Photo import failed", it)
                    importFailed.value = true
                }
        }
    }

    fun dismissError() {
        importFailed.value = false
    }

    fun deleteSelected() {
        val ids = selected.value
        selected.value = emptyList()
        viewModelScope.launch { ids.forEach { bodyRepository.deletePhoto(it) } }
    }

    /** The two selected photos as (before, after), oldest first. */
    fun comparePair(): Pair<String, String>? {
        val photos = uiState.value.photos.filter { it.id in uiState.value.selected }.sortedBy { it.date }
        return if (photos.size == 2) photos[0].id to photos[1].id else null
    }

    private companion object {
        const val TAG = "PhotosViewModel"
    }
}

data class CompareUiState(val before: ProgressPhoto? = null, val after: ProgressPhoto? = null, val units: UnitSystem = UnitSystem.METRIC)

@HiltViewModel
class PhotoCompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bodyRepository: BodyRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val beforeId: String = checkNotNull(savedStateHandle[BEFORE_ID_ARG])
    private val afterId: String = checkNotNull(savedStateHandle[AFTER_ID_ARG])

    val uiState: StateFlow<CompareUiState> = combine(
        bodyRepository.observePhotos(),
        preferencesRepository.userPreferences,
    ) { photos, prefs -> CompareUiState(photos.find { it.id == beforeId }, photos.find { it.id == afterId }, prefs.units) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState())
}
