package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
import com.chdjalil.jalilgym.core.database.dao.ProgressDao
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.progress.muscleSets
import com.chdjalil.jalilgym.core.domain.progress.sessionBests
import com.chdjalil.jalilgym.core.domain.progress.trainingStreak
import com.chdjalil.jalilgym.core.domain.repository.ProgressRepository
import com.chdjalil.jalilgym.core.model.ExerciseProgress
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.PersonalRecord
import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.StrengthPoint
import com.chdjalil.jalilgym.core.model.TrainingStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Weekly target when no program is active. */
internal const val DEFAULT_WEEKLY_TARGET = 2

internal fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

internal class OfflineFirstProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val exerciseDao: ExerciseDao,
    private val programDao: ProgramDao,
) : ProgressRepository {

    override fun observeExerciseProgress(): Flow<List<ExerciseProgress>> = combine(
        progressDao.observeExerciseHistory(),
        exerciseDao.observeAll(),
        progressDao.observeBestOneRepMax(),
    ) { history, exercises, bests ->
        val byId = exercises.associateBy { it.exercise.id }
        val best = bests.associate { it.exerciseId to it.value }
        history.mapNotNull { row ->
            val exercise = byId[row.exerciseId] ?: return@mapNotNull null
            ExerciseProgress(exercise.asExternalModel(), row.sessions, row.lastPerformedAt, best[row.exerciseId])
        }
    }

    override fun observeStrengthSeries(exerciseId: String): Flow<List<StrengthPoint>> =
        progressDao.observeCompletedSets(exerciseId).map { rows ->
            rows.groupBy { it.sessionId }.map { (sessionId, sessionRows) ->
                val bests = sessionBests(sessionRows.map { it.set.asExternalModel() }).associateBy { it.type }
                StrengthPoint(
                    sessionId = sessionId,
                    performedAt = sessionRows.first().startedAt,
                    bestEstimatedOneRepMaxKg = bests[PrType.EST_1RM]?.value,
                    topWeightKg = bests[PrType.MAX_WEIGHT]?.value,
                    volumeKg = bests[PrType.MAX_VOLUME]?.value ?: 0.0,
                )
            }.sortedBy { it.performedAt }
        }

    override fun observeRecords(exerciseId: String?): Flow<List<PersonalRecord>> =
        progressDao.observeRecords(exerciseId).map { rows ->
            rows.map {
                PersonalRecord(
                    id = it.record.id,
                    exerciseId = it.record.exerciseId,
                    exerciseName = it.exerciseName,
                    type = it.record.prType,
                    value = it.record.value,
                    achievedAt = it.record.achievedAt,
                    weightKg = it.weightKg.takeIf { _ -> it.record.prType != PrType.MAX_VOLUME },
                    reps = it.reps.takeIf { _ -> it.record.prType != PrType.MAX_VOLUME },
                )
            }
        }

    override fun observeWeeklyMuscleSets(weekStart: LocalDate): Flow<Map<Muscle, Double>> {
        val zone = ZoneId.systemDefault()
        val from = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return progressDao.observeFinishedBetween(from, until).map { sessions ->
            muscleSets(sessions.flatMap { it.asExternalModel().exercises })
        }
    }

    override fun observeTrainingStreak(): Flow<TrainingStreak> = combine(
        progressDao.observeWorkoutStarts(),
        programDao.observeActive(),
    ) { starts, active ->
        val target = active?.program?.program?.daysPerWeek ?: DEFAULT_WEEKLY_TARGET
        trainingStreak(starts.map { it.toLocalDate() }, target, LocalDate.now())
    }
}
