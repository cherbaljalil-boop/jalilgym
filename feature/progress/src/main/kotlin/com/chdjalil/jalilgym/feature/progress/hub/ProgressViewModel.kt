package com.chdjalil.jalilgym.feature.progress.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.progress.movingAverage
import com.chdjalil.jalilgym.core.domain.progress.weekStart
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.units.toKg
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrainingStreak
import com.chdjalil.jalilgym.core.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How far back the body-weight chart on the hub goes. */
internal const val HUB_WEIGHT_DAYS = 90L

data class WeekVolume(val weekStart: LocalDate, val isCurrentWeek: Boolean, val sets: Map<Muscle, Double>)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val units: UnitSystem = UnitSystem.METRIC,
    val streak: TrainingStreak? = null,
    val week: WeekVolume? = null,
    /** Weights within [HUB_WEIGHT_DAYS], oldest first. */
    val recentWeights: List<BodyWeight> = emptyList(),
    /** 7-day moving average of [recentWeights]. */
    val weightTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val latestWeight: BodyWeight? = null,
    /** Trend change over [recentWeights], in kg. */
    val weightChangeKg: Double? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    private val bodyRepository: BodyRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now()
    private val selectedWeek = MutableStateFlow(today.weekStart())

    private val week = selectedWeek.flatMapLatest { start ->
        progressRepository.observeWeeklyMuscleSets(start).map { WeekVolume(start, start == today.weekStart(), it) }
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        preferencesRepository.userPreferences,
        progressRepository.observeTrainingStreak(),
        week,
        bodyRepository.observeWeights(),
    ) { prefs, streak, week, weights ->
        val recent = weights.filter { it.date.isAfter(today.minusDays(HUB_WEIGHT_DAYS)) }
        val trend = movingAverage(recent.map { it.date to it.weightKg })
        ProgressUiState(
            isLoading = false,
            units = prefs.units,
            streak = streak,
            week = week,
            recentWeights = recent,
            weightTrend = trend,
            latestWeight = weights.lastOrNull(),
            weightChangeKg = if (trend.size >= 2) trend.last().second - trend.first().second else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun previousWeek() = selectedWeek.update { it.minusWeeks(1) }

    fun nextWeek() = selectedWeek.update { if (it < today.weekStart()) it.plusWeeks(1) else it }

    /** [value] is in the user's unit. */
    fun logWeight(value: Double) {
        val kg = uiState.value.units.toKg(value)
        viewModelScope.launch { bodyRepository.logWeight(today, kg) }
    }
}
