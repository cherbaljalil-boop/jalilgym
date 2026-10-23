package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
import com.chdjalil.jalilgym.core.database.dao.ProgressDao
import com.chdjalil.jalilgym.core.database.dao.WorkoutDao
import com.chdjalil.jalilgym.core.database.entity.PersonalRecordEntity
import com.chdjalil.jalilgym.core.database.entity.ProgressionStateEntity
import com.chdjalil.jalilgym.core.database.entity.SessionExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.SetLogEntity
import com.chdjalil.jalilgym.core.database.entity.WorkoutSessionEntity
import com.chdjalil.jalilgym.core.database.model.PopulatedSession
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.domain.program.nextRotationIndex
import com.chdjalil.jalilgym.core.domain.progress.newRecords
import com.chdjalil.jalilgym.core.domain.progress.sessionBests
import com.chdjalil.jalilgym.core.domain.repository.AchievementRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.domain.workout.ProgressionState
import com.chdjalil.jalilgym.core.domain.workout.applyProgression
import com.chdjalil.jalilgym.core.domain.workout.volumeKg
import com.chdjalil.jalilgym.core.model.ProgressionNote
import com.chdjalil.jalilgym.core.model.RecordNote
import com.chdjalil.jalilgym.core.model.PrType
import com.chdjalil.jalilgym.core.model.SessionStatus
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutHistoryItem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val progressDao: ProgressDao,
    private val achievementRepository: AchievementRepository,
) : WorkoutRepository {

    /** Serializes read-modify-write of set rows; see [editSet]. */
    private val setEdits = Mutex()

    /** Progression notes of workouts finished in this process, by session id. */
    private val progressionNotes = MutableStateFlow<Map<String, List<ProgressionNote>>>(emptyMap())

    /** Records and achievements from workouts finished in this process, by session id. */
    private val celebrations = MutableStateFlow<Map<String, Pair<List<RecordNote>, List<String>>>>(emptyMap())

    private fun now() = System.currentTimeMillis()

    private fun newId() = UUID.randomUUID().toString()

    /** Adds each exercise's sets from its previous finished session. */
    private suspend fun PopulatedSession.toModelWithPrevious(): WorkoutSession {
        val previous = exercises.map { it.entry.exerciseId }.distinct().associateWith { exerciseId ->
            workoutDao.lastCompletedSets(exerciseId, session.id).map { it.asExternalModel() }
        }
        return asExternalModel { previous[it].orEmpty() }
    }

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        workoutDao.observeActive().map { it?.toModelWithPrevious() }

    override fun observeSession(id: String): Flow<WorkoutSession?> =
        workoutDao.observeSession(id).map { it?.toModelWithPrevious() }

    override fun observeHistory(): Flow<List<WorkoutHistoryItem>> = workoutDao.observeHistory().map { rows ->
        rows.map {
            WorkoutHistoryItem(it.id, it.name, it.startedAt, it.endedAt, it.totalVolumeKg, it.exerciseCount, it.completedSets)
        }
    }

    override suspend fun startFromDay(dayId: String): String {
        workoutDao.activeIds().firstOrNull()?.let { return it }
        val day = checkNotNull(programDao.observeDay(dayId).first()) { "No program day $dayId" }
        val now = now()
        val sessionId = newId()
        val exercises = mutableListOf<SessionExerciseEntity>()
        val sets = mutableListOf<SetLogEntity>()
        day.exercises.filter { it.slot.deletedAt == null }.sortedBy { it.slot.position }.forEachIndexed { index, populated ->
            val slot = populated.slot
            val entryId = newId()
            exercises += SessionExerciseEntity(
                id = entryId,
                sessionId = sessionId,
                exerciseId = slot.exerciseId,
                position = index,
                supersetGroup = slot.supersetGroup,
                programExerciseId = slot.id,
                updatedAt = now,
            )
            val progression = workoutDao.getProgression(slot.id)
            val last = workoutDao.lastCompletedSets(slot.exerciseId, sessionId).filter { it.setType == SetType.WORKING }
            repeat(slot.targetSets) { i ->
                val previous = last.getOrNull(i) ?: last.lastOrNull()
                sets += SetLogEntity(
                    id = newId(),
                    sessionExerciseId = entryId,
                    position = i,
                    setType = SetType.WORKING,
                    weightKg = progression?.nextWeightKg ?: previous?.weightKg,
                    reps = progression?.nextTargetReps ?: previous?.reps ?: slot.repMin,
                    durationSec = null,
                    rpe = null,
                    rir = null,
                    completed = false,
                    completedAt = null,
                    updatedAt = now,
                )
            }
        }
        workoutDao.insertSessionTree(sessionEntity(sessionId, day.day.name, dayId, now), exercises, sets)
        return sessionId
    }

    override suspend fun startEmpty(name: String): String {
        workoutDao.activeIds().firstOrNull()?.let { return it }
        val id = newId()
        workoutDao.insertSessionTree(sessionEntity(id, name, null, now()), emptyList(), emptyList())
        return id
    }

    private fun sessionEntity(id: String, name: String, dayId: String?, now: Long) = WorkoutSessionEntity(
        id = id,
        programDayId = dayId,
        name = name,
        startedAt = now,
        endedAt = null,
        status = SessionStatus.ACTIVE,
        notes = null,
        totalVolumeKg = 0.0,
        healthConnectId = null,
        updatedAt = now,
    )

    override suspend fun addExercises(sessionId: String, exerciseIds: List<String>) {
        val now = now()
        val existing = workoutDao.getExercises(sessionId)
        val entries = mutableListOf<SessionExerciseEntity>()
        val sets = mutableListOf<SetLogEntity>()
        exerciseIds.forEachIndexed { offset, exerciseId ->
            if (exerciseDao.getById(exerciseId) == null) return@forEachIndexed
            val entryId = newId()
            entries += SessionExerciseEntity(entryId, sessionId, exerciseId, existing.size + offset, null, null, now)
            val last = workoutDao.lastCompletedSets(exerciseId, sessionId).filter { it.setType == SetType.WORKING }
            repeat(DEFAULT_AD_HOC_SETS) { i ->
                val previous = last.getOrNull(i) ?: last.lastOrNull()
                sets += newSet(entryId, i, SetType.WORKING, previous?.weightKg, previous?.reps, now)
            }
        }
        workoutDao.upsertExercises(entries)
        workoutDao.upsertSets(sets)
    }

    override suspend fun swapExercise(sessionExerciseId: String, exerciseId: String) {
        val entry = workoutDao.getExercise(sessionExerciseId) ?: return
        // A swapped exercise no longer follows the program slot's progression.
        workoutDao.upsertExercises(listOf(entry.copy(exerciseId = exerciseId, programExerciseId = null, updatedAt = now())))
    }

    override suspend fun removeExercise(sessionExerciseId: String) = workoutDao.deleteExercise(sessionExerciseId, now())

    override suspend fun addSet(sessionExerciseId: String, type: SetType): String {
        val sets = workoutDao.getSets(sessionExerciseId)
        val last = sets.lastOrNull()
        val set = newSet(sessionExerciseId, sets.size, type, last?.weightKg, last?.reps, now())
        workoutDao.upsertSets(listOf(set))
        return set.id
    }

    override suspend fun removeSet(setId: String) = workoutDao.deleteSet(setId, now())

    override suspend fun editSet(setId: String, transform: (WorkoutSet) -> WorkoutSet) = setEdits.withLock {
        val row = workoutDao.getSet(setId) ?: return@withLock
        val set = transform(row.asExternalModel())
        workoutDao.upsertSets(
            listOf(
                row.copy(
                    setType = set.type,
                    weightKg = set.weightKg,
                    reps = set.reps,
                    rpe = set.rpe,
                    rir = set.rir,
                    updatedAt = now(),
                ),
            ),
        )
    }

    override suspend fun setCompleted(setId: String, completed: Boolean) = setEdits.withLock {
        val row = workoutDao.getSet(setId) ?: return@withLock
        val now = now()
        workoutDao.upsertSets(listOf(row.copy(completed = completed, completedAt = if (completed) now else null, updatedAt = now)))
    }

    override suspend fun addWarmups(sessionExerciseId: String, warmups: List<Pair<Double, Int>>) {
        val now = now()
        val sets = warmups.mapIndexed { i, (kg, reps) -> newSet(sessionExerciseId, i, SetType.WARMUP, kg, reps, now) }
        workoutDao.prependSets(sessionExerciseId, sets, now)
    }

    override suspend fun finish(sessionId: String, notes: String?): WorkoutSummary {
        val populated = checkNotNull(workoutDao.getSession(sessionId)) { "No session $sessionId" }
        val now = now()

        // Unchecked sets weren't done; drop them (and exercises left empty) so history stays honest.
        populated.exercises.forEach { e ->
            e.sets.filterNot { it.completed }.forEach { workoutDao.deleteSetRow(it.id) }
            if (e.sets.none { it.completed }) workoutDao.deleteExerciseRow(e.entry.id)
        }
        val cleaned = checkNotNull(workoutDao.getSession(sessionId)).asExternalModel()
        val allSets = cleaned.exercises.flatMap { it.sets }
        val volume = volumeKg(allSets)
        workoutDao.upsertSession(
            populated.session.copy(
                status = SessionStatus.FINISHED,
                endedAt = now,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                totalVolumeKg = volume,
                updatedAt = now,
            ),
        )

        val notesList = cleaned.exercises.mapNotNull { exercise ->
            val slotId = exercise.programExerciseId ?: return@mapNotNull null
            val prescription = exercise.target ?: return@mapNotNull null
            val previous = workoutDao.getProgression(slotId)?.let {
                ProgressionState(it.nextWeightKg, it.nextTargetReps, it.consecutiveFailures)
            }
            val result = applyProgression(prescription, previous, exercise.sets) ?: return@mapNotNull null
            workoutDao.upsertProgression(
                ProgressionStateEntity(
                    programExerciseId = slotId,
                    nextWeightKg = result.state.nextWeightKg,
                    nextTargetReps = result.state.nextTargetReps,
                    consecutiveFailures = result.state.consecutiveFailures,
                    updatedAt = now,
                ),
            )
            ProgressionNote(
                exerciseName = exercise.exercise.name,
                outcome = result.outcome,
                fromWeightKg = result.fromWeightKg,
                toWeightKg = result.state.nextWeightKg,
                nextTargetReps = result.state.nextTargetReps,
            )
        }
        progressionNotes.update { it + (sessionId to notesList) }
        advanceRotationAfter(populated.session.programDayId)

        val records = recordPersonalBests(cleaned, now)
        val achievements = achievementRepository.syncUnlocks()
        celebrations.update { it + (sessionId to (records to achievements)) }

        val finished = checkNotNull(workoutDao.getSession(sessionId)).asExternalModel()
        return WorkoutSummary(
            session = finished,
            durationMillis = now - finished.startedAt,
            totalVolumeKg = volume,
            completedSets = allSets.count { it.completed },
            progression = notesList,
            records = records,
            newAchievements = achievements,
        )
    }

    /**
     * Stores each exercise's session bests that beat its records (first-time
     * values are stored as baselines) and returns the improvements.
     */
    private suspend fun recordPersonalBests(session: WorkoutSession, now: Long): List<RecordNote> =
        session.exercises.groupBy { it.exercise.id }.flatMap { (exerciseId, entries) ->
            val sets = entries.flatMap { it.sets }
            val candidates = sessionBests(sets)
            if (candidates.isEmpty()) return@flatMap emptyList()
            val previous = progressDao.bestRecords(exerciseId).associate { it.prType to it.value }
            val fresh = newRecords(candidates, previous)
            // Volume has no single set; anchor it to the heaviest so it's removed with the session.
            val anchor = candidates.first { it.type == PrType.MAX_WEIGHT }.set!!.id
            progressDao.insertRecords(
                fresh.map { (candidate, _) ->
                    PersonalRecordEntity(newId(), exerciseId, candidate.set?.id ?: anchor, candidate.type, candidate.value, now)
                },
            )
            fresh.map { (candidate, before) -> RecordNote(entries.first().exercise.name, candidate.type, candidate.value, before) }
                .filter { it.isImprovement }
        }

    /** The day after the one just trained comes next, if it belongs to the active program. */
    private suspend fun advanceRotationAfter(dayId: String?) {
        dayId ?: return
        val day = programDao.getDay(dayId) ?: return
        val active = programDao.getActive() ?: return
        if (active.programId != day.programId) return
        val dayCount = programDao.getDays(day.programId).size
        programDao.setActive(active.copy(nextDayIndex = nextRotationIndex(day.dayIndex, dayCount)))
    }

    override suspend fun discard(sessionId: String) = workoutDao.deleteSessionRow(sessionId)

    override suspend fun deleteSession(id: String) = workoutDao.deleteSessionRow(id)

    override fun observeSummary(sessionId: String): Flow<WorkoutSummary?> =
        combine(workoutDao.observeSession(sessionId), progressionNotes, celebrations) { populated, notes, celebrated ->
            populated?.takeIf { it.session.status == SessionStatus.FINISHED }?.let {
                val session = it.asExternalModel()
                val sets = session.exercises.flatMap { e -> e.sets }
                WorkoutSummary(
                    session = session,
                    durationMillis = (session.endedAt ?: session.startedAt) - session.startedAt,
                    totalVolumeKg = it.session.totalVolumeKg,
                    completedSets = sets.count { s -> s.completed },
                    progression = notes[sessionId].orEmpty(),
                    records = celebrated[sessionId]?.first.orEmpty(),
                    newAchievements = celebrated[sessionId]?.second.orEmpty(),
                )
            }
        }

    private fun newSet(entryId: String, position: Int, type: SetType, weight: Double?, reps: Int?, now: Long) = SetLogEntity(
        id = newId(),
        sessionExerciseId = entryId,
        position = position,
        setType = type,
        weightKg = weight,
        reps = reps,
        durationSec = null,
        rpe = null,
        rir = null,
        completed = false,
        completedAt = null,
        updatedAt = now,
    )

    private companion object {
        const val DEFAULT_AD_HOC_SETS = 3
    }
}
