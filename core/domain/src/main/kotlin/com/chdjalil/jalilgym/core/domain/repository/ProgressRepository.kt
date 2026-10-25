package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.ExerciseProgress
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.TrainingStreak
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Read-only views over finished workouts (docs/BLUEPRINT.md §3.5). */
interface ProgressRepository {
    /** Exercises with at least one finished session, most recently trained first. */
    fun observeExerciseProgress(): Flow<List<ExerciseProgress>>

    /** One point per finished session with this exercise, oldest first. */
    fun observeStrengthSeries(exerciseId: String): Flow<List<StrengthPoint>>

    /** Record history, newest first; all exercises when [exerciseId] is null. */
    fun observeRecords(exerciseId: String? = null): Flow<List<PersonalRecord>>

    /** Working sets per muscle for workouts started in the week beginning [weekStart] (a Monday). */
    fun observeWeeklyMuscleSets(weekStart: LocalDate): Flow<Map<Muscle, Double>>

    fun observeTrainingStreak(): Flow<TrainingStreak>
}
