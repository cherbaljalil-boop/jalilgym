package com.chdjalil.jalilgym.feature.progress.strength

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.ExerciseProgress
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.feature.progress.navigation.EXERCISE_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StrengthListUiState(
    val isLoading: Boolean = true,
    val exercises: List<ExerciseProgress> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
)

@HiltViewModel
class StrengthListViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<StrengthListUiState> = combine(
        progressRepository.observeExerciseProgress(),
        preferencesRepository.userPreferences,
    ) { exercises, prefs -> StrengthListUiState(false, exercises, prefs.units) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StrengthListUiState())
}

enum class StrengthMetric { EST_1RM, TOP_WEIGHT, VOLUME }

enum class ChartRange(val days: Long?) { WEEKS_4(28), MONTHS_3(91), MONTHS_6(182), YEAR(365), ALL(null) }

/** The value [metric] plots for this session, if it has one. */
fun StrengthPoint.valueOf(metric: StrengthMetric): Double? = when (metric) {
    StrengthMetric.EST_1RM -> bestEstimatedOneRepMaxKg
    StrengthMetric.TOP_WEIGHT -> topWeightKg
    StrengthMetric.VOLUME -> volumeKg.takeIf { it > 0 }
}

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val exercise: Exercise? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val metric: StrengthMetric = StrengthMetric.EST_1RM,
    val range: ChartRange = ChartRange.MONTHS_3,
    /** Sessions in [range] that have a value for [metric], oldest first. */
    val points: List<Pair<Long, Double>> = emptyList(),
    val sessionCount: Int = 0,
    val records: List<PersonalRecord> = emptyList(),
)

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
    progressRepository: ProgressRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle[EXERCISE_ID_ARG])
    private val metric = MutableStateFlow(StrengthMetric.EST_1RM)
    private val range = MutableStateFlow(ChartRange.MONTHS_3)

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        combine(exerciseRepository.observeExercise(exerciseId), preferencesRepository.userPreferences) { e, p -> e to p.units },
        progressRepository.observeStrengthSeries(exerciseId),
        progressRepository.observeRecords(exerciseId),
        metric,
        range,
    ) { (exercise, units), series, records, metric, range ->
        val since = range.days?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it) } ?: Long.MIN_VALUE
        ExerciseProgressUiState(
            isLoading = false,
            exercise = exercise,
            units = units,
            metric = metric,
            range = range,
            points = series.filter { it.performedAt >= since }.mapNotNull { p -> p.valueOf(metric)?.let { p.performedAt to it } },
            sessionCount = series.size,
            records = records,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    fun selectMetric(value: StrengthMetric) {
        metric.value = value
    }

    fun selectRange(value: ChartRange) {
        range.value = value
    }
}
