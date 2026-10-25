package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.chdjalil.jalilgym.core.database.entity.AchievementUnlockEntity
import com.chdjalil.jalilgym.core.database.entity.PersonalRecordEntity
import com.chdjalil.jalilgym.core.database.entity.SetLogEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedSession
import com.chdjalil.jalilgym.core.model.PrType
import kotlinx.coroutines.flow.Flow

/** A completed set with the session it belongs to. */
data class SessionSetRow(
    val sessionId: String,
    val startedAt: Long,
    @Embedded val set: SetLogEntity,
)

data class ExerciseHistoryRow(val exerciseId: String, val sessions: Int, val lastPerformedAt: Long)

data class RecordRow(
    @Embedded val record: PersonalRecordEntity,
    val exerciseName: String,
    val weightKg: Double?,
    val reps: Int?,
)

data class BestValueRow(val exerciseId: String, val value: Double)

data class TypeValueRow(val prType: PrType, val value: Double)

private const val FINISHED = "s.status = 'FINISHED' AND s.deletedAt IS NULL"

@Dao
interface ProgressDao {

    @Query(
        """
        SELECT s.id AS sessionId, s.startedAt AS startedAt, l.* FROM set_logs l
        JOIN session_exercises e ON l.sessionExerciseId = e.id
        JOIN workout_sessions s ON e.sessionId = s.id
        WHERE e.exerciseId = :exerciseId AND $FINISHED AND l.completed = 1
        ORDER BY s.startedAt, l.position
        """,
    )
    fun observeCompletedSets(exerciseId: String): Flow<List<SessionSetRow>>

    @Query(
        """
        SELECT e.exerciseId AS exerciseId, COUNT(DISTINCT s.id) AS sessions, MAX(s.startedAt) AS lastPerformedAt
        FROM session_exercises e JOIN workout_sessions s ON e.sessionId = s.id
        WHERE $FINISHED
        GROUP BY e.exerciseId
        ORDER BY lastPerformedAt DESC
        """,
    )
    fun observeExerciseHistory(): Flow<List<ExerciseHistoryRow>>

    @Query(
        """
        SELECT r.*, x.name AS exerciseName, l.weightKg AS weightKg, l.reps AS reps
        FROM personal_records r
        JOIN exercises x ON x.id = r.exerciseId
        LEFT JOIN set_logs l ON l.id = r.setLogId
        WHERE (:exerciseId IS NULL OR r.exerciseId = :exerciseId)
        ORDER BY r.achievedAt DESC
        """,
    )
    fun observeRecords(exerciseId: String?): Flow<List<RecordRow>>

    @Query("SELECT prType, MAX(value) AS value FROM personal_records WHERE exerciseId = :exerciseId GROUP BY prType")
    suspend fun bestRecords(exerciseId: String): List<TypeValueRow>

    @Query("SELECT exerciseId, MAX(value) AS value FROM personal_records WHERE prType = 'EST_1RM' GROUP BY exerciseId")
    fun observeBestOneRepMax(): Flow<List<BestValueRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<PersonalRecordEntity>)

    /** Records that beat an earlier one (first-time baselines aren't counted). */
    @Query(
        """
        SELECT COUNT(*) FROM personal_records r WHERE EXISTS (
            SELECT 1 FROM personal_records p
            WHERE p.exerciseId = r.exerciseId AND p.prType = r.prType AND p.achievedAt < r.achievedAt
        )
        """,
    )
    fun observeImprovedRecordCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM workout_sessions s WHERE $FINISHED AND s.startedAt >= :from AND s.startedAt < :until")
    fun observeFinishedBetween(from: Long, until: Long): Flow<List<PopulatedSession>>

    @Query("SELECT s.startedAt FROM workout_sessions s WHERE $FINISHED ORDER BY s.startedAt")
    fun observeWorkoutStarts(): Flow<List<Long>>

    @Query("SELECT COALESCE(SUM(s.totalVolumeKg), 0) FROM workout_sessions s WHERE $FINISHED")
    fun observeTotalVolume(): Flow<Double>

    @Query("SELECT COUNT(*) FROM programs WHERE isTemplate = 0 AND sourceTemplateId IS NULL AND deletedAt IS NULL")
    fun observeCustomProgramCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM exercises WHERE isBuiltIn = 0 AND deletedAt IS NULL")
    fun observeCustomExerciseCount(): Flow<Int>

    @Query("SELECT * FROM achievement_unlocks")
    fun observeUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlocks(unlocks: List<AchievementUnlockEntity>)
}
