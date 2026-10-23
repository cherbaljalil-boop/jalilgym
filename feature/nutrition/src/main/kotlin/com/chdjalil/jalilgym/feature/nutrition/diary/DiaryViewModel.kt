package com.chdjalil.jalilgym.feature.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.model.DiaryDay
import com.chdjalil.jalilgym.core.model.DietPlan
import com.chdjalil.jalilgym.core.model.MealType
import com.chdjalil.jalilgym.core.model.Nutrients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DiaryUiState(
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val day: DiaryDay = DiaryDay(LocalDate.now(), emptyList()),
    /** Null until nutrition setup is done. */
    val plan: DietPlan? = null,
    val loggingStreak: Int = 0,
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
) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())
    private val _messages = MutableSharedFlow<DiaryMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<DiaryMessage> = _messages.asSharedFlow()

    val uiState: StateFlow<DiaryUiState> = combine(
        date.flatMapLatest { nutritionRepository.observeDiary(it) },
        nutritionRepository.observeActivePlan(),
        nutritionRepository.observeLoggingStreak(),
    ) { day, plan, streak ->
        DiaryUiState(
            isLoading = false,
            date = day.date,
            isToday = day.date == LocalDate.now(),
            day = day,
            plan = plan,
            loggingStreak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun previousDay() = date.update { it.minusDays(1) }

    fun nextDay() = date.update { it.plusDays(1) }

    fun goToToday() {
        date.value = LocalDate.now()
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
