package com.chdjalil.jalilgym.feature.progress.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.progress.movingAverage
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.units.toKg
import com.chdjalil.jalilgym.core.model.BodyMeasurement
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.progress.ui.toCm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Latest value of one measurement and its change since the previous one. */
data class MeasurementSummary(val type: MeasurementType, val latest: BodyMeasurement, val change: Double?)

data class BodyUiState(
    val isLoading: Boolean = true,
    val units: UnitSystem = UnitSystem.METRIC,
    /** Oldest first. */
    val weights: List<BodyWeight> = emptyList(),
    val weightTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val measurements: List<MeasurementSummary> = emptyList(),
    /** Newest first. */
    val measurementHistory: List<BodyMeasurement> = emptyList(),
)

@HiltViewModel
class BodyViewModel @Inject constructor(
    private val bodyRepository: BodyRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<BodyUiState> = combine(
        preferencesRepository.userPreferences,
        bodyRepository.observeWeights(),
        bodyRepository.observeMeasurements(),
    ) { prefs, weights, measurements ->
        BodyUiState(
            isLoading = false,
            units = prefs.units,
            weights = weights,
            weightTrend = movingAverage(weights.map { it.date to it.weightKg }),
            measurements = summarize(measurements),
            measurementHistory = measurements,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    /** [value] is in the user's unit. */
    fun logWeight(value: Double, date: LocalDate = LocalDate.now()) {
        val kg = uiState.value.units.toKg(value)
        viewModelScope.launch { bodyRepository.logWeight(date, kg) }
    }

    fun deleteWeight(id: String) {
        viewModelScope.launch { bodyRepository.deleteWeight(id) }
    }

    /** [value] is in the user's unit (in, cm or %). */
    fun logMeasurement(type: MeasurementType, value: Double, date: LocalDate = LocalDate.now()) {
        val stored = uiState.value.units.toCm(type, value)
        viewModelScope.launch { bodyRepository.logMeasurement(date, type, stored) }
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch { bodyRepository.deleteMeasurement(id) }
    }

    private fun summarize(measurements: List<BodyMeasurement>): List<MeasurementSummary> =
        MeasurementType.entries.mapNotNull { type ->
            val history = measurements.filter { it.type == type }.sortedByDescending { it.date }
            val latest = history.firstOrNull() ?: return@mapNotNull null
            MeasurementSummary(type, latest, history.getOrNull(1)?.let { latest.value - it.value })
        }
}
