package com.chdjalil.jalilgym.feature.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.nutrition.CheckIn
import com.chdjalil.jalilgym.core.domain.nutrition.CheckInVerdict
import com.chdjalil.jalilgym.core.domain.nutrition.adjustTargets
import com.chdjalil.jalilgym.core.domain.nutrition.checkInDue
import com.chdjalil.jalilgym.core.domain.nutrition.minimumCalories
import com.chdjalil.jalilgym.core.domain.nutrition.weeklyCheckIn
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.Sex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val day: DiaryDay = DiaryDay(LocalDate.now(), emptyList()),
    /** Null until nutrition setup is done. */
    val plan: DietPlan? = null,
    val loggingStreak: Int = 0,
    /** This week's check-in, when one is due and there's enough weight data. */
    val checkIn: CheckIn? = null,
)

/** One-off messages for a snackbar. */
sealed interface DiaryMessage {
    data class Copied(val count: Int) : DiaryMessage

    data object NothingToCopy : DiaryMessage

    data object MealSaved : DiaryMessage

    data object NothingToSave : DiaryMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    bodyRepository: BodyRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())
    private val _messages = MutableSharedFlow<DiaryMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<DiaryMessage> = _messages.asSharedFlow()

    private val checkIn = combine(
        nutritionRepository.observeActivePlan(),
        bodyRepository.observeWeights(),
        preferencesRepository.userPreferences,
    ) { plan, weights, prefs ->
        val today = LocalDate.now()
        if (plan == null || !checkInDue(prefs.lastCheckIn, today)) return@combine null
        weeklyCheckIn(weights.map { it.date to it.weightKg }, plan.goal, today)
            .takeIf { it.verdict != CheckInVerdict.NOT_ENOUGH_DATA }
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        date.flatMapLatest { nutritionRepository.observeDiary(it) },
        nutritionRepository.observeActivePlan(),
        nutritionRepository.observeLoggingStreak(),
        checkIn,
    ) { day, plan, streak, checkIn ->
        DiaryUiState(
            isLoading = false,
            date = day.date,
            isToday = day.date == LocalDate.now(),
            day = day,
            plan = plan,
            loggingStreak = streak,
            checkIn = checkIn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun previousDay() = date.update { it.minusDays(1) }

    fun nextDay() = date.update { it.plusDays(1) }

    fun goToToday() {
        date.value = LocalDate.now()
    }

    /** Applies the suggested calorie change; carbs absorb it. */
    fun applyCheckIn() {
        val state = uiState.value
        val plan = state.plan ?: return
        val adjustment = state.checkIn?.adjustmentKcal ?: return
        viewModelScope.launch {
            if (adjustment != 0) {
                val sex = preferencesRepository.userPreferences.first().bodyProfile?.sex ?: Sex.FEMALE
                nutritionRepository.savePlan(
                    goal = plan.goal,
                    style = plan.style,
                    halalOnly = plan.halalOnly,
                    targets = adjustTargets(plan.targets, adjustment, minimumCalories(sex)),
                    isManualOverride = true,
                )
            }
            preferencesRepository.setLastCheckIn(LocalDate.now())
        }
    }

    /** Keeps the targets as they are until next week. */
    fun dismissCheckIn() {
        viewModelScope.launch { preferencesRepository.setLastCheckIn(LocalDate.now()) }
    }

    fun quickAdd(meal: MealType, name: String, nutrients: Nutrients) {
        viewModelScope.launch { nutritionRepository.quickAdd(date.value, meal, name, nutrients) }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { nutritionRepository.deleteEntry(id) }
    }

    /** Copies the same meal from the day before the one shown. */
    fun copyFromPreviousDay(meal: MealType) {
        val target = date.value
        viewModelScope.launch {
            val count = nutritionRepository.copyMeal(target.minusDays(1), target, meal)
            _messages.emit(if (count > 0) DiaryMessage.Copied(count) else DiaryMessage.NothingToCopy)
        }
    }

    fun saveMeal(meal: MealType, name: String) {
        val day = date.value
        viewModelScope.launch {
            val id = nutritionRepository.saveMealFromDiary(name, day, meal)
            _messages.emit(if (id != null) DiaryMessage.MealSaved else DiaryMessage.NothingToSave)
        }
    }
}
