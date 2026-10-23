package com.chdjalil.jalilgym.feature.workout.active

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.domain.workout.CompleteSetUseCase
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UserPreferences
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.ui.ExercisePickerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ActiveWorkoutUiState {
    data object Loading : ActiveWorkoutUiState

    /** No workout is running (finished elsewhere, or discarded). */
    data object None : ActiveWorkoutUiState

    data class Active(
        val session: WorkoutSession,
        val rest: RestTimerState?,
        val preferences: UserPreferences,
    ) : ActiveWorkoutUiState
}

sealed interface ActiveWorkoutEvent {
    /** Applies [transform] to the latest stored values of the set. */
    data class EditSet(val setId: String, val transform: (WorkoutSet) -> WorkoutSet) : ActiveWorkoutEvent

    /** Cycles working → warm-up → drop → failure → working. */
    data class CycleType(val set: WorkoutSet) : ActiveWorkoutEvent

    data class ToggleComplete(val set: WorkoutSet) : ActiveWorkoutEvent

    data class AddSet(val sessionExerciseId: String) : ActiveWorkoutEvent

    data class RemoveSet(val setId: String) : ActiveWorkoutEvent

    /** Warm-ups as (kg, reps), inserted before the working sets. */
    data class AddWarmups(val sessionExerciseId: String, val warmups: List<Pair<Double, Int>>) : ActiveWorkoutEvent

    data class RemoveExercise(val sessionExerciseId: String) : ActiveWorkoutEvent

    /** Remembers which exercise to swap; the picker result then replaces it. */
    data class RequestSwap(val sessionExerciseId: String) : ActiveWorkoutEvent

    data object RequestAdd : ActiveWorkoutEvent

    data class AdjustRest(val seconds: Int) : ActiveWorkoutEvent

    data object SkipRest : ActiveWorkoutEvent

    data class Finish(val notes: String?) : ActiveWorkoutEvent

    data object Discard : ActiveWorkoutEvent
}

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val restTimer: RestTimer,
    private val completeSet: CompleteSetUseCase,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        workoutRepository.observeActiveSession(),
        restTimer.state,
        preferencesRepository.userPreferences,
    ) { session, rest, prefs ->
        if (session == null) ActiveWorkoutUiState.None else ActiveWorkoutUiState.Active(session, rest, prefs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState.Loading)

    private val _finishedSessionId = MutableStateFlow<String?>(null)

    /** Set once the workout was finished; the screen opens its summary. */
    val finishedSessionId: StateFlow<String?> = _finishedSessionId.asStateFlow()

    private val _discarded = MutableStateFlow(false)
    val discarded: StateFlow<Boolean> = _discarded.asStateFlow()

    /** Label for the rest timer, e.g. "Bench Press · set 2 of 4"; provided by the screen (string resources). */
    var restLabel: (String, Int, Int) -> String = { name, set, total -> "$name · $set/$total" }

    init {
        // Exercises picked in the exercise picker come back through this SavedStateHandle.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<List<String>?>(ExercisePickerResult.KEY, null)
                .filterNotNull()
                .collect { ids ->
                    savedStateHandle[ExercisePickerResult.KEY] = null
                    val swapTarget = savedStateHandle.get<String>(SWAP_TARGET_KEY)
                    savedStateHandle[SWAP_TARGET_KEY] = null
                    if (swapTarget != null) {
                        ids.firstOrNull()?.let { workoutRepository.swapExercise(swapTarget, it) }
                    } else {
                        val session = workoutRepository.observeActiveSession().first() ?: return@collect
                        workoutRepository.addExercises(session.id, ids)
                    }
                }
        }
    }

    fun onEvent(event: ActiveWorkoutEvent) {
        viewModelScope.launch {
            when (event) {
                is ActiveWorkoutEvent.EditSet -> workoutRepository.editSet(event.setId, event.transform)
                is ActiveWorkoutEvent.CycleType -> workoutRepository.editSet(event.set.id) { it.copy(type = it.type.next()) }
                is ActiveWorkoutEvent.ToggleComplete ->
                    if (event.set.completed) {
                        workoutRepository.setCompleted(event.set.id, false)
                    } else {
                        completeSet(event.set.id, restLabel)
                    }
                is ActiveWorkoutEvent.AddSet -> workoutRepository.addSet(event.sessionExerciseId)
                is ActiveWorkoutEvent.RemoveSet -> workoutRepository.removeSet(event.setId)
                is ActiveWorkoutEvent.AddWarmups -> workoutRepository.addWarmups(event.sessionExerciseId, event.warmups)
                is ActiveWorkoutEvent.RemoveExercise -> workoutRepository.removeExercise(event.sessionExerciseId)
                is ActiveWorkoutEvent.RequestSwap -> savedStateHandle[SWAP_TARGET_KEY] = event.sessionExerciseId
                ActiveWorkoutEvent.RequestAdd -> savedStateHandle[SWAP_TARGET_KEY] = null
                is ActiveWorkoutEvent.AdjustRest -> restTimer.add(event.seconds)
                ActiveWorkoutEvent.SkipRest -> restTimer.skip()
                is ActiveWorkoutEvent.Finish -> {
                    val session = workoutRepository.observeActiveSession().first() ?: return@launch
                    restTimer.skip()
                    _finishedSessionId.value = workoutRepository.finish(session.id, event.notes).session.id
                }
                ActiveWorkoutEvent.Discard -> {
                    val session = workoutRepository.observeActiveSession().first() ?: return@launch
                    restTimer.skip()
                    workoutRepository.discard(session.id)
                    _discarded.value = true
                }
            }
        }
    }

    private companion object {
        const val SWAP_TARGET_KEY = "swap_target"
    }
}

internal fun SetType.next(): SetType = when (this) {
    SetType.WORKING -> SetType.WARMUP
    SetType.WARMUP -> SetType.DROP
    SetType.DROP -> SetType.FAILURE
    SetType.FAILURE -> SetType.WORKING
}
