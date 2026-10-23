package com.chdjalil.jalilgym.core.domain.workout

import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Marks a set done and starts the right rest timer. Shared by the workout
 * screen and the notification's "Complete set" action.
 */
class CompleteSetUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val restTimer: RestTimer,
) {
    /**
     * Completes [setId] in the active workout, or the next incomplete set when null.
     * [labelFor] builds the timer label for what comes next (exercise name, set number of total).
     * Returns false when there was nothing to complete.
     */
    suspend operator fun invoke(setId: String? = null, labelFor: (String, Int, Int) -> String): Boolean {
        val session = workoutRepository.observeActiveSession().first() ?: return false
        val (exercise, set) = if (setId == null) {
            session.nextIncompleteSet() ?: return false
        } else {
            session.exercises.firstNotNullOfOrNull { e -> e.sets.find { it.id == setId }?.let { e to it } } ?: return false
        }
        workoutRepository.setCompleted(set.id, true)

        val prefs = preferencesRepository.userPreferences.first()
        val rest = session.restAfter(exercise, set, prefs.defaultRestCompoundSec, prefs.defaultRestIsolationSec)
        if (rest != null) {
            val completedIds = session.exercises.flatMap { it.sets }.filter { it.completed }.map { it.id }.toSet() + set.id
            val upNext = session.copy(
                exercises = session.exercises.map { e ->
                    e.copy(sets = e.sets.map { if (it.id in completedIds) it.copy(completed = true) else it })
                },
            ).nextIncompleteSet()
            val label = upNext?.let { (e, s) ->
                labelFor(e.exercise.name, e.sets.sortedBy { it.position }.indexOf(s) + 1, e.sets.size)
            }.orEmpty()
            restTimer.start(rest, label)
        }
        return true
    }
}
