package com.chdjalil.jalilgym.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.ExerciseRepository
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import com.chdjalil.jalilgym.core.domain.repository.NutritionRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.MacroTargets
import com.chdjalil.jalilgym.core.model.Nutrients
import com.chdjalil.jalilgym.core.model.TrainingStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val isLoading: Boolean = true,
    val exerciseCount: Int? = null,
    val active: ActiveProgram? = null,
    /** Name of the workout in progress, if any. */
    val workoutInProgress: String? = null,
    val streak: TrainingStreak? = null,
    /** What's been eaten today. */
    val eatenToday: Nutrients = Nutrients.ZERO,
    /** Null until nutrition setup is done. */
    val targets: MacroTargets? = null,
    /** Today's steps from Health Connect, when connected. */
    val steps: Long? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
    workoutRepository: WorkoutRepository,
    progressRepository: ProgressRepository,
    nutritionRepository: NutritionRepository,
    private val healthRepository: HealthRepository,
) : ViewModel() {

    private val nutrition = combine(
        nutritionRepository.observeDiary(LocalDate.now()),
        nutritionRepository.observeActivePlan(),
    ) { day, plan -> day.totals to plan?.targets }

    private val training = combine(
        programRepository.observeActiveProgram(),
        workoutRepository.observeActiveSession(),
        progressRepository.observeTrainingStreak(),
    ) { active, session, streak -> Triple(active, session?.name, streak) }

    val uiState: StateFlow<TodayUiState> = combine(
        exerciseRepository.observeExerciseCount(),
        training,
        nutrition,
        healthRepository.observeStepsToday(),
    ) { count, (active, session, streak), (eaten, targets), steps ->
        TodayUiState(
            isLoading = false,
            exerciseCount = count,
            active = active,
            workoutInProgress = session,
            streak = streak,
            eatenToday = eaten,
            targets = targets,
            steps = steps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun refreshSteps() {
        viewModelScope.launch { healthRepository.refreshSteps() }
    }

    /** Skip today's planned workout and move on to the next one in the rotation. */
    fun skipToNextDay() {
        viewModelScope.launch { programRepository.advanceRotation() }
    }
}
