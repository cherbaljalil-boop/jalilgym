package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.program.defaultPrescription
import com.chdjalil.jalilgym.core.domain.program.nextRotationIndex
import com.chdjalil.jalilgym.core.domain.program.toggleSupersetLink
import com.chdjalil.jalilgym.core.domain.repository.ProgramRepository
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramExercise
import com.chdjalil.jalilgym.core.model.ProgramType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [ProgramRepository] that mirrors the real one's rules. */
class FakeProgramRepository(
    private val exercises: List<Exercise> = emptyList(),
    templates: List<Program> = emptyList(),
) : ProgramRepository {

    private val programs = MutableStateFlow(templates)
    private val active = MutableStateFlow<Pair<String, Int>?>(null)
    private var nextId = 1

    val currentPrograms: List<Program> get() = programs.value
    val activeProgramId: String? get() = active.value?.first
    val activeNextDayIndex: Int? get() = active.value?.second

    private fun newId(prefix: String) = "$prefix-${nextId++}"

    override fun observeTemplates(): Flow<List<Program>> = programs.map { all -> all.filter { it.isTemplate } }

    override fun observeUserPrograms(): Flow<List<Program>> =
        programs.map { all -> all.filterNot { it.isTemplate }.reversed() }

    override fun observeProgram(id: String): Flow<Program?> = programs.map { all -> all.find { it.id == id } }

    override fun observeDay(dayId: String): Flow<ProgramDay?> =
        programs.map { all -> all.flatMap { it.days }.find { it.id == dayId } }

    override fun observeActiveProgram(): Flow<ActiveProgram?> = combine(programs, active) { all, current ->
        current?.let { (id, index) -> all.find { it.id == id }?.let { ActiveProgram(it, index, startedAt = 0L) } }
    }

    override suspend fun copyProgram(sourceId: String, name: String?): String {
        val source = programs.value.first { it.id == sourceId }
        val programId = newId("program")
        val copy = source.copy(
            id = programId,
            name = name ?: source.name,
            isTemplate = false,
            sourceTemplateId = if (source.isTemplate) source.id else source.sourceTemplateId,
            days = source.days.map { day ->
                val dayId = newId("day")
                day.copy(
                    id = dayId,
                    programId = programId,
                    exercises = day.exercises.map { it.copy(id = newId("pe"), dayId = dayId) },
                )
            },
        )
        programs.update { it + copy }
        return programId
    }

    override suspend fun createProgram(name: String): String {
        val programId = newId("program")
        val day = ProgramDay(newId("day"), programId, 0, "Day 1", emptyList())
        programs.update { it + Program(programId, name, ProgramType.CUSTOM, 3, false, null, listOf(day)) }
        return programId
    }

    override suspend fun renameProgram(id: String, name: String) = updateProgram(id) { it.copy(name = name) }

    override suspend fun setDaysPerWeek(id: String, daysPerWeek: Int) =
        updateProgram(id) { it.copy(daysPerWeek = daysPerWeek) }

    override suspend fun deleteProgram(id: String) {
        programs.update { all -> all.filterNot { it.id == id } }
        if (active.value?.first == id) active.value = null
    }

    override suspend fun addDay(programId: String, name: String): String {
        val dayId = newId("day")
        updateProgram(programId) { it.copy(days = (it.days + ProgramDay(dayId, programId, 0, name, emptyList())).reindexed()) }
        return dayId
    }

    override suspend fun renameDay(dayId: String, name: String) = updateDay(dayId) { it.copy(name = name) }

    override suspend fun duplicateDay(dayId: String): String {
        val newDayId = newId("day")
        val program = programOf(dayId)
        updateProgram(program.id) { p ->
            val index = p.days.indexOfFirst { it.id == dayId }
            val source = p.days[index]
            val copy = source.copy(
                id = newDayId,
                name = "${source.name} (copy)",
                exercises = source.exercises.map { it.copy(id = newId("pe"), dayId = newDayId) },
            )
            p.copy(days = (p.days.take(index + 1) + copy + p.days.drop(index + 1)).reindexed())
        }
        return newDayId
    }

    override suspend fun deleteDay(dayId: String) {
        val program = programOf(dayId)
        updateProgram(program.id) { p -> p.copy(days = p.days.filterNot { it.id == dayId }.reindexed()) }
    }

    override suspend fun moveDay(dayId: String, offset: Int) {
        val program = programOf(dayId)
        updateProgram(program.id) { p -> p.copy(days = p.days.moved(p.days.indexOfFirst { it.id == dayId }, offset).reindexed()) }
    }

    override suspend fun addExercises(dayId: String, exerciseIds: List<String>) = updateDay(dayId) { day ->
        val added = exerciseIds.map { exerciseId ->
            val exercise = exercises.first { it.id == exerciseId }
            val p = defaultPrescription(exercise, goal = null, restCompoundSec = 180, restIsolationSec = 90)
            ProgramExercise(
                id = newId("pe"), dayId = dayId, exercise = exercise, position = 0,
                targetSets = p.targetSets, repMin = p.repMin, repMax = p.repMax, targetRpe = p.targetRpe,
                restSeconds = p.restSeconds, supersetGroup = null, progressionRule = p.progressionRule,
                incrementKg = p.incrementKg, deloadAfterFailures = p.deloadAfterFailures, deloadPercent = p.deloadPercent,
            )
        }
        day.copy(exercises = (day.exercises + added).repositioned())
    }

    override suspend fun updatePrescription(programExerciseId: String, prescription: Prescription) =
        updateExercise(programExerciseId) {
            it.copy(
                targetSets = prescription.targetSets,
                repMin = prescription.repMin,
                repMax = prescription.repMax,
                targetRpe = prescription.targetRpe,
                restSeconds = prescription.restSeconds,
                progressionRule = prescription.progressionRule,
                incrementKg = prescription.incrementKg,
                deloadAfterFailures = prescription.deloadAfterFailures,
                deloadPercent = prescription.deloadPercent,
            )
        }

    override suspend fun removeExercise(programExerciseId: String) {
        val dayId = dayOfExercise(programExerciseId)
        updateDay(dayId) { day -> day.copy(exercises = day.exercises.filterNot { it.id == programExerciseId }.repositioned()) }
    }

    override suspend fun moveExercise(programExerciseId: String, offset: Int) {
        val dayId = dayOfExercise(programExerciseId)
        updateDay(dayId) { day ->
            day.copy(exercises = day.exercises.moved(day.exercises.indexOfFirst { it.id == programExerciseId }, offset).repositioned())
        }
    }

    override suspend fun toggleSupersetWithNext(dayId: String, index: Int) = updateDay(dayId) { day ->
        val groups = toggleSupersetLink(day.exercises.map { it.id }, day.exercises.map { it.supersetGroup }, index)
        day.copy(exercises = day.exercises.mapIndexed { i, e -> e.copy(supersetGroup = groups[i]) })
    }

    override suspend fun activate(programId: String) {
        active.value = programId to 0
    }

    override suspend fun deactivate() {
        active.value = null
    }

    override suspend fun setNextDay(dayIndex: Int) {
        active.update { it?.copy(second = dayIndex) }
    }

    override suspend fun advanceRotation() {
        val (id, index) = active.value ?: return
        val days = programs.value.first { it.id == id }.days.size
        active.value = id to nextRotationIndex(index, days)
    }

    private fun programOf(dayId: String) = programs.value.first { p -> p.days.any { it.id == dayId } }

    private fun dayOfExercise(id: String) = programs.value.flatMap { it.days }.first { d -> d.exercises.any { it.id == id } }.id

    private fun updateProgram(id: String, transform: (Program) -> Program) =
        programs.update { all -> all.map { if (it.id == id) transform(it) else it } }

    private fun updateDay(dayId: String, transform: (ProgramDay) -> ProgramDay) =
        programs.update { all -> all.map { p -> p.copy(days = p.days.map { if (it.id == dayId) transform(it) else it }) } }

    private fun updateExercise(id: String, transform: (ProgramExercise) -> ProgramExercise) =
        programs.update { all ->
            all.map { p -> p.copy(days = p.days.map { d -> d.copy(exercises = d.exercises.map { if (it.id == id) transform(it) else it }) }) }
        }

    private fun List<ProgramDay>.reindexed() = mapIndexed { i, d -> d.copy(dayIndex = i) }

    private fun List<ProgramExercise>.repositioned() = mapIndexed { i, e -> e.copy(position = i) }

    private fun <T> List<T>.moved(from: Int, offset: Int): List<T> {
        val to = (from + offset).coerceIn(0, size - 1)
        if (from == to) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }
}
