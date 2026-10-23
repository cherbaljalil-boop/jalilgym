package com.chdjalil.jalilgym.feature.programs.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.Program
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateGalleryUiState(
    val isLoading: Boolean = true,
    val templates: List<Program> = emptyList(),
)

@HiltViewModel
class TemplateGalleryViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
) : ViewModel() {

    val uiState: StateFlow<TemplateGalleryUiState> = programRepository.observeTemplates()
        .map { TemplateGalleryUiState(isLoading = false, templates = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplateGalleryUiState())

    private val _createdProgramId = MutableStateFlow<String?>(null)
    val createdProgramId: StateFlow<String?> = _createdProgramId.asStateFlow()

    /** Copies the template into an editable program; it becomes active if nothing else is. */
    fun useTemplate(templateId: String) {
        if (_createdProgramId.value != null) return
        viewModelScope.launch {
            val id = programRepository.copyProgram(templateId)
            if (programRepository.observeActiveProgram().first() == null) programRepository.activate(id)
            _createdProgramId.value = id
        }
    }
}
