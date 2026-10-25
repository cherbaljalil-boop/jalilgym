package com.chdjalil.jalilgym.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.chdjalil.jalilgym.core.database.entity.ActiveProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramDayEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.model.ActiveProgram
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramExercise

data class PopulatedProgramExercise(
    @Embedded val slot: ProgramExerciseEntity,
    @Relation(entity = ExerciseEntity::class, parentColumn = "exerciseId", entityColumn = "id")
    val exercise: PopulatedExercise,
)

data class PopulatedProgramDay(
    @Embedded val day: ProgramDayEntity,
    @Relation(entity = ProgramExerciseEntity::class, parentColumn = "id", entityColumn = "programDayId")
    val exercises: List<PopulatedProgramExercise>,
)

data class PopulatedProgram(
    @Embedded val program: ProgramEntity,
    @Relation(entity = ProgramDayEntity::class, parentColumn = "id", entityColumn = "programId")
    val days: List<PopulatedProgramDay>,
)

data class PopulatedActiveProgram(
    @Embedded val active: ActiveProgramEntity,
    @Relation(entity = ProgramEntity::class, parentColumn = "programId", entityColumn = "id")
    val program: PopulatedProgram,
)

fun PopulatedProgramExercise.asExternalModel() = ProgramExercise(
    id = slot.id,
    dayId = slot.programDayId,
    exercise = exercise.asExternalModel(),
    position = slot.position,
    targetSets = slot.targetSets,
    repMin = slot.repMin,
    repMax = slot.repMax,
    targetRpe = slot.targetRpe,
    restSeconds = slot.restSeconds,
    supersetGroup = slot.supersetGroup,
    progressionRule = slot.progressionRule,
    incrementKg = slot.incrementKg,
    deloadAfterFailures = slot.deloadAfterFailures,
    deloadPercent = slot.deloadPercent,
)

fun PopulatedProgramDay.asExternalModel() = ProgramDay(
    id = day.id,
    programId = day.programId,
    dayIndex = day.dayIndex,
    name = day.name,
    exercises = exercises.filter { it.slot.deletedAt == null }.sortedBy { it.slot.position }.map { it.asExternalModel() },
)

fun PopulatedProgram.asExternalModel() = Program(
    id = program.id,
    name = program.name,
    type = program.type,
    daysPerWeek = program.daysPerWeek,
    isTemplate = program.isTemplate,
    sourceTemplateId = program.sourceTemplateId,
    days = days.filter { it.day.deletedAt == null }.sortedBy { it.day.dayIndex }.map { it.asExternalModel() },
)

fun PopulatedActiveProgram.asExternalModel() = ActiveProgram(
    program = program.asExternalModel(),
    nextDayIndex = active.nextDayIndex,
    startedAt = active.startedAt,
)
