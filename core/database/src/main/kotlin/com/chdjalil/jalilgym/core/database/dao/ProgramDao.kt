package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.ActiveProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramDayEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedActiveProgram
import com.chdjalil.jalilgym.core.database.model.PopulatedProgram
import com.chdjalil.jalilgym.core.database.model.PopulatedProgramDay
import kotlinx.coroutines.flow.Flow

/**
 * Programs, their days and exercise slots. Multi-row edits are single
 * transactions so a program is never observed half-updated. Days and slots are
 * hard-deleted (logged sessions keep their own copies via SET NULL); programs
 * are soft-deleted.
 */
@Dao
interface ProgramDao {

    @Transaction
    @Query("SELECT * FROM programs WHERE isTemplate = 1 AND deletedAt IS NULL ORDER BY daysPerWeek, name")
    fun observeTemplates(): Flow<List<PopulatedProgram>>

    @Transaction
    @Query("SELECT * FROM programs WHERE isTemplate = 0 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeUserPrograms(): Flow<List<PopulatedProgram>>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id AND deletedAt IS NULL")
    fun observeProgram(id: String): Flow<PopulatedProgram?>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getProgram(id: String): PopulatedProgram?

    @Transaction
    @Query("SELECT * FROM program_days WHERE id = :dayId")
    fun observeDay(dayId: String): Flow<PopulatedProgramDay?>

    @Query("SELECT * FROM program_days WHERE id = :dayId")
    suspend fun getDay(dayId: String): ProgramDayEntity?

    @Query("SELECT * FROM program_days WHERE programId = :programId ORDER BY dayIndex")
    suspend fun getDays(programId: String): List<ProgramDayEntity>

    @Query("SELECT * FROM program_exercises WHERE programDayId = :dayId ORDER BY position")
    suspend fun getSlots(dayId: String): List<ProgramExerciseEntity>

    @Query("SELECT * FROM program_exercises WHERE id = :id")
    suspend fun getSlot(id: String): ProgramExerciseEntity?

    @Transaction
    @Query("SELECT * FROM active_program WHERE singleton = 0")
    fun observeActive(): Flow<PopulatedActiveProgram?>

    @Query("SELECT * FROM active_program WHERE singleton = 0")
    suspend fun getActive(): ActiveProgramEntity?

    @Upsert
    suspend fun upsertPrograms(programs: List<ProgramEntity>)

    @Upsert
    suspend fun upsertDays(days: List<ProgramDayEntity>)

    @Upsert
    suspend fun upsertSlots(slots: List<ProgramExerciseEntity>)

    @Query("DELETE FROM program_days WHERE id = :dayId")
    suspend fun deleteDayRow(dayId: String)

    @Query("DELETE FROM program_days WHERE programId IN (:programIds)")
    suspend fun deleteDaysOf(programIds: List<String>)

    @Query("DELETE FROM program_exercises WHERE id = :slotId")
    suspend fun deleteSlotRow(slotId: String)

    @Query("UPDATE programs SET deletedAt = :now, updatedAt = :now WHERE id = :id AND isTemplate = 0")
    suspend fun softDeleteProgram(id: String, now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActive(active: ActiveProgramEntity)

    @Query("DELETE FROM active_program")
    suspend fun clearActive()

    /** Replaces a program's whole tree (used by seed import and copies). */
    @Transaction
    suspend fun replaceProgramTree(
        program: ProgramEntity,
        days: List<ProgramDayEntity>,
        slots: List<ProgramExerciseEntity>,
    ) {
        upsertPrograms(listOf(program))
        deleteDaysOf(listOf(program.id))
        upsertDays(days)
        upsertSlots(slots)
    }

    @Transaction
    suspend fun deleteProgram(id: String, now: Long) {
        softDeleteProgram(id, now)
        if (getActive()?.programId == id) clearActive()
    }

    /** Removes a day and closes the gap in the rotation. */
    @Transaction
    suspend fun deleteDay(dayId: String, now: Long) {
        val day = getDay(dayId) ?: return
        deleteDayRow(dayId)
        val remaining = getDays(day.programId)
        upsertDays(remaining.mapIndexed { index, d -> d.copy(dayIndex = index, updatedAt = now) })
        val active = getActive()
        if (active?.programId == day.programId && active.nextDayIndex >= remaining.size) {
            setActive(active.copy(nextDayIndex = 0))
        }
    }

    /** Writes days in the given order (dayIndex = list position). */
    @Transaction
    suspend fun reorderDays(days: List<ProgramDayEntity>, now: Long) {
        upsertDays(days.mapIndexed { index, d -> d.copy(dayIndex = index, updatedAt = now) })
    }

    /** Writes a day's slots in the given order (position = list position). */
    @Transaction
    suspend fun reorderSlots(slots: List<ProgramExerciseEntity>, now: Long) {
        upsertSlots(slots.mapIndexed { index, s -> s.copy(position = index, updatedAt = now) })
    }

    @Transaction
    suspend fun deleteSlot(slotId: String, now: Long) {
        val slot = getSlot(slotId) ?: return
        deleteSlotRow(slotId)
        reorderSlots(getSlots(slot.programDayId), now)
    }

    @Transaction
    suspend fun insertDayCopy(day: ProgramDayEntity, slots: List<ProgramExerciseEntity>, now: Long) {
        val days = getDays(day.programId).toMutableList()
        days.add((day.dayIndex).coerceIn(0, days.size), day)
        upsertDays(listOf(day))
        upsertSlots(slots)
        reorderDays(days, now)
    }
}
