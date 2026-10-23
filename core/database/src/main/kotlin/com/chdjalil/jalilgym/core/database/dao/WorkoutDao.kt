package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.ProgressionStateEntity
import com.chdjalil.jalilgym.core.database.entity.SessionExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.SetLogEntity
import com.chdjalil.jalilgym.core.database.entity.WorkoutSessionEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedSession
import kotlinx.coroutines.flow.Flow

/** Row for history lists, aggregated in SQL. */
data class SessionHistoryRow(
    val id: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val totalVolumeKg: Double,
    val exerciseCount: Int,
    val completedSets: Int,
)

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<PopulatedSession?>

    @Query("SELECT id FROM workout_sessions WHERE status = 'ACTIVE' AND deletedAt IS NULL")
    suspend fun activeIds(): List<String>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id AND deletedAt IS NULL")
    fun observeSession(id: String): Flow<PopulatedSession?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: String): PopulatedSession?

    @Query(
        """
        SELECT s.id, s.name, s.startedAt, s.endedAt, s.totalVolumeKg,
            (SELECT COUNT(*) FROM session_exercises e WHERE e.sessionId = s.id) AS exerciseCount,
            (SELECT COUNT(*) FROM set_logs l JOIN session_exercises e ON l.sessionExerciseId = e.id
                WHERE e.sessionId = s.id AND l.completed = 1) AS completedSets
        FROM workout_sessions s
        WHERE s.status = 'FINISHED' AND s.deletedAt IS NULL AND s.endedAt IS NOT NULL
        ORDER BY s.startedAt DESC
        """,
    )
    fun observeHistory(): Flow<List<SessionHistoryRow>>

    /**
     * Completed sets from the most recent finished session (other than
     * [excludeSessionId]) that included [exerciseId], in set order. Used for
     * "previous" values and pre-fill.
     */
    @Query(
        """
        SELECT l.* FROM set_logs l
        JOIN session_exercises e ON l.sessionExerciseId = e.id
        WHERE e.id = (
            SELECT e2.id FROM session_exercises e2
            JOIN workout_sessions s ON e2.sessionId = s.id
            WHERE e2.exerciseId = :exerciseId AND s.status = 'FINISHED' AND s.deletedAt IS NULL
                AND s.id != :excludeSessionId
            ORDER BY s.startedAt DESC, e2.position LIMIT 1
        ) AND l.completed = 1
        ORDER BY l.position
        """,
    )
    suspend fun lastCompletedSets(exerciseId: String, excludeSessionId: String): List<SetLogEntity>

    @Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<SessionExerciseEntity>)

    @Upsert
    suspend fun upsertSets(sets: List<SetLogEntity>)

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getExercise(id: String): SessionExerciseEntity?

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("SELECT * FROM set_logs WHERE id = :id")
    suspend fun getSet(id: String): SetLogEntity?

    @Query("SELECT * FROM set_logs WHERE sessionExerciseId = :sessionExerciseId ORDER BY position")
    suspend fun getSets(sessionExerciseId: String): List<SetLogEntity>

    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteExerciseRow(id: String)

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetRow(id: String)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionRow(id: String)

    @Query("SELECT * FROM progression_states WHERE programExerciseId = :programExerciseId")
    suspend fun getProgression(programExerciseId: String): ProgressionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgression(state: ProgressionStateEntity)

    @Transaction
    suspend fun insertSessionTree(
        session: WorkoutSessionEntity,
        exercises: List<SessionExerciseEntity>,
        sets: List<SetLogEntity>,
    ) {
        upsertSession(session)
        upsertExercises(exercises)
        upsertSets(sets)
    }

    @Transaction
    suspend fun deleteExercise(id: String, now: Long) {
        val entry = getExercise(id) ?: return
        deleteExerciseRow(id)
        upsertExercises(getExercises(entry.sessionId).mapIndexed { i, e -> e.copy(position = i, updatedAt = now) })
    }

    @Transaction
    suspend fun deleteSet(id: String, now: Long) {
        val set = getSet(id) ?: return
        deleteSetRow(id)
        upsertSets(getSets(set.sessionExerciseId).mapIndexed { i, s -> s.copy(position = i, updatedAt = now) })
    }

    /** Puts [sets] first and renumbers the rest after them. */
    @Transaction
    suspend fun prependSets(sessionExerciseId: String, sets: List<SetLogEntity>, now: Long) {
        val existing = getSets(sessionExerciseId)
        upsertSets((sets + existing).mapIndexed { i, s -> s.copy(position = i, updatedAt = now) })
    }
}
