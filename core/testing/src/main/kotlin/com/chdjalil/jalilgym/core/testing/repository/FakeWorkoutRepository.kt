package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.domain.workout.volumeKg
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SessionStatus
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.model.WorkoutSummary
import com.chdjalil.jalilgym.core.model.prescription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [WorkoutRepository]; progression isn't simulated (see the domain tests for that). */
class FakeWorkoutRepository(
    private val exercises: List<Exercise> = emptyList(),
    private val programs: FakeProgramRepository? = null,
    var now: Long = 1_000L,
) : WorkoutRepository {

    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private var nextId = 1
    private fun newId(prefix: String) = "$prefix-${nextId++}"

    val current: List<WorkoutSession> get() = sessions.value

    override fun observeActiveSession(): Flow<WorkoutSession?> = sessions.map { all -> all.find { it.isActive } }

    override fun observeSession(id: String): Flow<WorkoutSession?> = sessions.map { all -> all.find { it.id == id } }

    override fun observeHistory(): Flow<List<WorkoutHistoryItem>> = sessions.map { all ->
        all.filter { it.status == SessionStatus.FINISHED }.sortedByDescending { it.startedAt }.map { s ->
            WorkoutHistoryItem(
                s.id, s.name, s.startedAt, s.endedAt ?: s.startedAt,
                volumeKg(s.exercises.flatMap { it.sets }), s.exercises.size,
                s.exercises.sumOf { e -> e.sets.count { it.completed } },
            )
        }
    }

    override suspend fun startFromDay(dayId: String): String {
        val day = checkNotNull(programs).currentPrograms.flatMap { it.days }.first { it.id == dayId }
        val id = newId("session")
        val sessionExercises = day.exercises.map { slot ->
            SessionExercise(
                id = newId("se"),
                exercise = slot.exercise,
                position = slot.position,
                supersetGroup = slot.supersetGroup,
                programExerciseId = slot.id,
                target = slot.prescription,
                sets = List(slot.targetSets) { i -> blankSet(i, weight = null, reps = slot.repMin) },
            )
        }
        sessions.update { it + WorkoutSession(id, day.name, dayId, now, null, SessionStatus.ACTIVE, null, sessionExercises) }
        return id
    }

    override suspend fun startEmpty(name: String): String {
        val id = newId("session")
        sessions.update { it + WorkoutSession(id, name, null, now, null, SessionStatus.ACTIVE, null, emptyList()) }
        return id
    }

    override suspend fun addExercises(sessionId: String, exerciseIds: List<String>) = updateSession(sessionId) { s ->
        val added = exerciseIds.mapIndexed { i, exerciseId ->
            SessionExercise(
                id = newId("se"),
                exercise = exercises.first { it.id == exerciseId },
                position = s.exercises.size + i,
                supersetGroup = null,
                programExerciseId = null,
                target = null,
                sets = List(3) { blankSet(it, null, null) },
            )
        }
        s.copy(exercises = s.exercises + added)
    }

    override suspend fun swapExercise(sessionExerciseId: String, exerciseId: String) =
        updateExercise(sessionExerciseId) { it.copy(exercise = exercises.first { e -> e.id == exerciseId }) }

    override suspend fun removeExercise(sessionExerciseId: String) = sessions.update { all ->
        all.map { s -> s.copy(exercises = s.exercises.filterNot { it.id == sessionExerciseId }) }
    }

    override suspend fun addSet(sessionExerciseId: String, type: SetType): String {
        val id = newId("set")
        updateExercise(sessionExerciseId) { e ->
            val last = e.sets.maxByOrNull { it.position }
            e.copy(sets = e.sets + blankSet(e.sets.size, last?.weightKg, last?.reps).copy(id = id, type = type))
        }
        return id
    }

    override suspend fun removeSet(setId: String) = sessions.update { all ->
        all.map { s -> s.copy(exercises = s.exercises.map { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) }) }
    }

    override suspend fun editSet(setId: String, transform: (WorkoutSet) -> WorkoutSet) = updateSetRow(setId) {
        val changed = transform(it)
        it.copy(type = changed.type, weightKg = changed.weightKg, reps = changed.reps, rpe = changed.rpe, rir = changed.rir)
    }

    override suspend fun setCompleted(setId: String, completed: Boolean) = updateSetRow(setId) {
        it.copy(completed = completed, completedAt = if (completed) now else null)
    }

    override suspend fun addWarmups(sessionExerciseId: String, warmups: List<Pair<Double, Int>>) =
        updateExercise(sessionExerciseId) { e ->
            val warm = warmups.mapIndexed { i, (kg, reps) -> blankSet(i, kg, reps).copy(type = SetType.WARMUP) }
            e.copy(sets = (warm + e.sets).mapIndexed { i, s -> s.copy(position = i) })
        }

    override suspend fun finish(sessionId: String, notes: String?): WorkoutSummary {
        updateSession(sessionId) { it.copy(status = SessionStatus.FINISHED, endedAt = now, notes = notes) }
        programs?.advanceRotation()
        val session = sessions.value.first { it.id == sessionId }
        return summaryOf(session)
    }

    override suspend fun discard(sessionId: String) = updateSession(sessionId) { it.copy(status = SessionStatus.DISCARDED) }

    override suspend fun deleteSession(id: String) = sessions.update { all -> all.filterNot { it.id == id } }

    override fun observeSummary(sessionId: String): Flow<WorkoutSummary?> =
        sessions.map { all -> all.find { it.id == sessionId && it.status == SessionStatus.FINISHED }?.let(::summaryOf) }

    private fun summaryOf(session: WorkoutSession): WorkoutSummary {
        val sets = session.exercises.flatMap { it.sets }
        return WorkoutSummary(
            session = session,
            durationMillis = (session.endedAt ?: now) - session.startedAt,
            totalVolumeKg = volumeKg(sets),
            completedSets = sets.count { it.completed },
            progression = emptyList(),
        )
    }

    private fun blankSet(position: Int, weight: Double?, reps: Int?) =
        WorkoutSet(newId("set"), position, SetType.WORKING, weight, reps, null, null, completed = false, completedAt = null)

    private fun updateSession(id: String, transform: (WorkoutSession) -> WorkoutSession) =
        sessions.update { all -> all.map { if (it.id == id) transform(it) else it } }

    private fun updateExercise(id: String, transform: (SessionExercise) -> SessionExercise) =
        sessions.update { all -> all.map { s -> s.copy(exercises = s.exercises.map { if (it.id == id) transform(it) else it }) } }

    private fun updateSetRow(id: String, transform: (WorkoutSet) -> WorkoutSet) =
        sessions.update { all ->
            all.map { s -> s.copy(exercises = s.exercises.map { e -> e.copy(sets = e.sets.map { if (it.id == id) transform(it) else it }) }) }
        }
}
