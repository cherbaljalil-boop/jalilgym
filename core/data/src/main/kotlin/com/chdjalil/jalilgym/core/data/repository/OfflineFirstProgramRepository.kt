package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.ExerciseDao
import com.chdjalil.jalilgym.core.database.dao.ProgramDao
import com.chdjalil.jalilgym.core.database.entity.ActiveProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramDayEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.database.model.asExternalModel
import com.chdjalil.jalilgym.core.datastore.JgPreferencesDataSource
import com.chdjalil.jalilgym.core.domain.program.defaultPrescription
import com.chdjalil.jalilgym.core.domain.program.nextRotationIndex
import com.chdjalil.jalilgym.core.domain.program.toggleSupersetLink
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramType
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class OfflineFirstProgramRepository @Inject constructor(
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val preferences: JgPreferencesDataSource,
) : ProgramRepository {

    private fun now() = System.currentTimeMillis()

    private fun newId() = UUID.randomUUID().toString()

    override fun observeTemplates(): Flow<List<Program>> =
        programDao.observeTemplates().map { list -> list.map { it.asExternalModel() } }

    override fun observeUserPrograms(): Flow<List<Program>> =
        programDao.observeUserPrograms().map { list -> list.map { it.asExternalModel() } }

    override fun observeProgram(id: String): Flow<Program?> = programDao.observeProgram(id).map { it?.asExternalModel() }

    override fun observeDay(dayId: String): Flow<ProgramDay?> = programDao.observeDay(dayId).map { it?.asExternalModel() }

    override fun observeActiveProgram(): Flow<ActiveProgram?> =
        programDao.observeActive().map { active -> active?.takeIf { it.program.program.deletedAt == null }?.asExternalModel() }

    override suspend fun copyProgram(sourceId: String, name: String?): String {
        val source = checkNotNull(programDao.getProgram(sourceId)) { "No program $sourceId" }
        val now = now()
        val programId = newId()
        val program = source.program.copy(
            id = programId,
            name = name ?: source.program.name,
            isTemplate = false,
            sourceTemplateId = if (source.program.isTemplate) source.program.id else source.program.sourceTemplateId,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        val days = source.days.filter { it.day.deletedAt == null }.sortedBy { it.day.dayIndex }
        val dayIds = days.associate { it.day.id to newId() }
        val dayEntities = days.mapIndexed { index, d ->
            d.day.copy(id = dayIds.getValue(d.day.id), programId = programId, dayIndex = index, updatedAt = now)
        }
        val slots = days.flatMap { d ->
            d.exercises.map { it.slot }.sortedBy { it.position }.map { slot ->
                slot.copy(id = newId(), programDayId = dayIds.getValue(slot.programDayId), updatedAt = now)
            }
        }
        programDao.replaceProgramTree(program, dayEntities, slots)
        return programId
    }

    override suspend fun createProgram(name: String): String {
        val now = now()
        val programId = newId()
        programDao.replaceProgramTree(
            ProgramEntity(
                id = programId,
                name = name,
                type = ProgramType.CUSTOM,
                daysPerWeek = 3,
                goalTag = null,
                isTemplate = false,
                sourceTemplateId = null,
                createdAt = now,
                updatedAt = now,
            ),
            listOf(ProgramDayEntity(id = newId(), programId = programId, dayIndex = 0, name = DEFAULT_DAY_NAME, updatedAt = now)),
            emptyList(),
        )
        return programId
    }

    override suspend fun renameProgram(id: String, name: String) = updateProgram(id) { it.copy(name = name) }

    override suspend fun setDaysPerWeek(id: String, daysPerWeek: Int) =
        updateProgram(id) { it.copy(daysPerWeek = daysPerWeek.coerceIn(1, 7)) }

    private suspend fun updateProgram(id: String, transform: (ProgramEntity) -> ProgramEntity) {
        val program = programDao.getProgram(id)?.program ?: return
        require(!program.isTemplate) { "Templates are read-only" }
        programDao.upsertPrograms(listOf(transform(program).copy(updatedAt = now())))
    }

    override suspend fun deleteProgram(id: String) = programDao.deleteProgram(id, now())

    override suspend fun addDay(programId: String, name: String): String {
        val id = newId()
        val index = programDao.getDays(programId).size
        programDao.upsertDays(listOf(ProgramDayEntity(id, programId, index, name, updatedAt = now())))
        return id
    }

    override suspend fun renameDay(dayId: String, name: String) {
        val day = programDao.getDay(dayId) ?: return
        programDao.upsertDays(listOf(day.copy(name = name, updatedAt = now())))
    }

    override suspend fun duplicateDay(dayId: String): String {
        val day = checkNotNull(programDao.getDay(dayId))
        val now = now()
        val copyId = newId()
        val copy = day.copy(id = copyId, dayIndex = day.dayIndex + 1, name = "${day.name} (copy)", updatedAt = now)
        val slots = programDao.getSlots(dayId).map { it.copy(id = newId(), programDayId = copyId, updatedAt = now) }
        programDao.insertDayCopy(copy, slots, now)
        return copyId
    }

    override suspend fun deleteDay(dayId: String) = programDao.deleteDay(dayId, now())

    override suspend fun moveDay(dayId: String, offset: Int) {
        val day = programDao.getDay(dayId) ?: return
        val days = programDao.getDays(day.programId)
        programDao.reorderDays(days.moved(days.indexOfFirst { it.id == dayId }, offset), now())
    }

    override suspend fun addExercises(dayId: String, exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        val prefs = preferences.userPreferences.first()
        val existing = programDao.getSlots(dayId)
        val now = now()
        val slots = exerciseIds.mapIndexedNotNull { offset, exerciseId ->
            val exercise = exerciseDao.getById(exerciseId)?.asExternalModel() ?: return@mapIndexedNotNull null
            val p = defaultPrescription(exercise, prefs.goal, prefs.defaultRestCompoundSec, prefs.defaultRestIsolationSec)
            ProgramExerciseEntity(
                id = newId(),
                programDayId = dayId,
                exerciseId = exerciseId,
                position = existing.size + offset,
                targetSets = p.targetSets,
                repMin = p.repMin,
                repMax = p.repMax,
                targetRpe = p.targetRpe,
                restSeconds = p.restSeconds,
                supersetGroup = null,
                progressionRule = p.progressionRule,
                incrementKg = p.incrementKg,
                deloadAfterFailures = p.deloadAfterFailures,
                deloadPercent = p.deloadPercent,
                updatedAt = now,
            )
        }
        programDao.reorderSlots(existing + slots, now)
    }

    override suspend fun updatePrescription(programExerciseId: String, prescription: Prescription) {
        val slot = programDao.getSlot(programExerciseId) ?: return
        programDao.upsertSlots(
            listOf(
                slot.copy(
                    targetSets = prescription.targetSets.coerceIn(1, 20),
                    repMin = prescription.repMin.coerceAtLeast(1),
                    repMax = prescription.repMax.coerceAtLeast(prescription.repMin.coerceAtLeast(1)),
                    targetRpe = prescription.targetRpe,
                    restSeconds = prescription.restSeconds.coerceIn(0, 60 * 15),
                    progressionRule = prescription.progressionRule,
                    incrementKg = prescription.incrementKg.coerceAtLeast(0.0),
                    deloadAfterFailures = prescription.deloadAfterFailures.coerceIn(1, 10),
                    deloadPercent = prescription.deloadPercent.coerceIn(0.0, 0.5),
                    updatedAt = now(),
                ),
            ),
        )
    }

    override suspend fun removeExercise(programExerciseId: String) = programDao.deleteSlot(programExerciseId, now())

    override suspend fun moveExercise(programExerciseId: String, offset: Int) {
        val slot = programDao.getSlot(programExerciseId) ?: return
        val slots = programDao.getSlots(slot.programDayId)
        programDao.reorderSlots(slots.moved(slots.indexOfFirst { it.id == programExerciseId }, offset), now())
    }

    override suspend fun toggleSupersetWithNext(dayId: String, index: Int) {
        val slots = programDao.getSlots(dayId)
        val groups = toggleSupersetLink(slots.map { it.id }, slots.map { it.supersetGroup }, index)
        programDao.reorderSlots(slots.mapIndexed { i, s -> s.copy(supersetGroup = groups[i]) }, now())
    }

    override suspend fun activate(programId: String) {
        programDao.setActive(ActiveProgramEntity(programId = programId, nextDayIndex = 0, startedAt = now()))
    }

    override suspend fun deactivate() = programDao.clearActive()

    override suspend fun setNextDay(dayIndex: Int) {
        val active = programDao.getActive() ?: return
        val dayCount = programDao.getDays(active.programId).size
        programDao.setActive(active.copy(nextDayIndex = dayIndex.coerceIn(0, (dayCount - 1).coerceAtLeast(0))))
    }

    override suspend fun advanceRotation() {
        val active = programDao.getActive() ?: return
        val dayCount = programDao.getDays(active.programId).size
        programDao.setActive(active.copy(nextDayIndex = nextRotationIndex(active.nextDayIndex, dayCount)))
    }

    private fun <T> List<T>.moved(from: Int, offset: Int): List<T> {
        if (from !in indices) return this
        val to = (from + offset).coerceIn(0, size - 1)
        if (from == to) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }

    private companion object {
        const val DEFAULT_DAY_NAME = "Day 1"
    }
}
