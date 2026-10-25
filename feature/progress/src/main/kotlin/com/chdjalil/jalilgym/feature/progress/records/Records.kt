package com.chdjalil.jalilgym.feature.progress.records

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.ui.labelRes
import com.chdjalil.jalilgym.feature.progress.R
import com.chdjalil.jalilgym.feature.progress.strength.EmptyMessage
import com.chdjalil.jalilgym.feature.progress.ui.BackBar
import com.chdjalil.jalilgym.feature.progress.ui.formatDate
import com.chdjalil.jalilgym.feature.progress.ui.formatWeight
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecordsUiState(
    val isLoading: Boolean = true,
    val records: List<PersonalRecord> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<RecordsUiState> = combine(
        progressRepository.observeRecords(),
        preferencesRepository.userPreferences,
    ) { records, prefs -> RecordsUiState(false, records, prefs.units) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())
}

@Composable
internal fun RecordsScreen(onBack: () -> Unit, viewModel: RecordsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackBar(stringResource(R.string.feature_progress_records_title), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                uiState.records.isEmpty() -> EmptyMessage(stringResource(R.string.feature_progress_records_empty))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(uiState.records, key = { it.id }) { RecordRow(it, uiState.units, showExercise = true) }
                }
            }
        }
    }
}

/** One record: "Estimated 1RM · 102.5 kg" with how it was set and when. */
@Composable
internal fun RecordRow(record: PersonalRecord, units: UnitSystem, showExercise: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (showExercise) Text(record.exerciseName, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(record.type.labelRes),
                style = if (showExercise) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            )
            val detail = if (record.weightKg != null && record.reps != null) {
                stringResource(R.string.feature_progress_record_set, formatNumber(units.fromKg(record.weightKg!!)), record.reps!!) + " · "
            } else {
                ""
            }
            Text(
                detail + formatDate(record.achievedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(units.formatWeight(record.value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
