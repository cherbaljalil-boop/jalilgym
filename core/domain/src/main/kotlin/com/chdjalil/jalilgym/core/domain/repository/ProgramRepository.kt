package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    fun observeTemplates(): Flow<List<Program>>

    /** The user's own programs (never templates), most recently created first. */
    fun observeUserPrograms(): Flow<List<Program>>

    fun observeProgram(id: String): Flow<Program?>

    fun observeDay(dayId: String): Flow<ProgramDay?>

    fun observeActiveProgram(): Flow<ActiveProgram?>

    /** Deep-copies a template (or any program) into a new editable program and returns its id. */
    suspend fun copyProgram(sourceId: String, name: String? = null): String

    /** Creates an empty custom program with one day and returns its id. */
    suspend fun createProgram(name: String): String

    suspend fun renameProgram(id: String, name: String)

    suspend fun setDaysPerWeek(id: String, daysPerWeek: Int)

    suspend fun deleteProgram(id: String)

    suspend fun addDay(programId: String, name: String): String

    suspend fun renameDay(dayId: String, name: String)

    suspend fun duplicateDay(dayId: String): String

    suspend fun deleteDay(dayId: String)

    /** Moves a day by [offset] positions in the rotation (negative = earlier). */
    suspend fun moveDay(dayId: String, offset: Int)

    /** Appends exercises to a day with sensible default prescriptions. */
    suspend fun addExercises(dayId: String, exerciseIds: List<String>)

    suspend fun updatePrescription(programExerciseId: String, prescription: Prescription)

    suspend fun removeExercise(programExerciseId: String)

    /** Moves an exercise by [offset] positions within its day. */
    suspend fun moveExercise(programExerciseId: String, offset: Int)

    /** Links (or unlinks) the exercise at [index] in its day with the next one as a superset. */
    suspend fun toggleSupersetWithNext(dayId: String, index: Int)

    suspend fun activate(programId: String)

    suspend fun deactivate()

    /** Sets which day comes next, e.g. when the user wants to train a different day today. */
    suspend fun setNextDay(dayIndex: Int)

    /** Moves the rotation on after a workout (or a skipped day). */
    suspend fun advanceRotation()
}
