package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.model.ExerciseProgress
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.TrainingStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeProgressRepository : ProgressRepository {
    val exercises = MutableStateFlow<List<ExerciseProgress>>(emptyList())
    val series = MutableStateFlow<Map<String, List<StrengthPoint>>>(emptyMap())
    val records = MutableStateFlow<List<PersonalRecord>>(emptyList())

    /** Muscle sets by week start. */
    val weeklySets = MutableStateFlow<Map<LocalDate, Map<Muscle, Double>>>(emptyMap())
    val streak = MutableStateFlow(TrainingStreak(0, 0, 0, 2, null))

    override fun observeExerciseProgress(): Flow<List<ExerciseProgress>> = exercises

    override fun observeStrengthSeries(exerciseId: String): Flow<List<StrengthPoint>> = series.map { it[exerciseId].orEmpty() }

    override fun observeRecords(exerciseId: String?): Flow<List<PersonalRecord>> =
        records.map { list -> list.filter { exerciseId == null || it.exerciseId == exerciseId } }

    override fun observeWeeklyMuscleSets(weekStart: LocalDate): Flow<Map<Muscle, Double>> =
        weeklySets.map { it[weekStart].orEmpty() }

    override fun observeTrainingStreak(): Flow<TrainingStreak> = streak
}
