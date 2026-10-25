package com.chdjalil.jalilgym.core.database.seed

import com.chdjalil.jalilgym.core.database.entity.ProgramDayEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.model.ProgressionRule
import kotlinx.serialization.Serializable

/** Shape of `assets/seed/programs.json`. Bump [version] whenever the file changes. */
@Serializable
data class ProgramSeedFile(
    val version: Int,
    val programs: List<SeedProgram>,
)

@Serializable
data class SeedProgram(
    val id: String,
    val name: String,
    val type: ProgramType,
    val daysPerWeek: Int,
    val goalTag: String? = null,
    val days: List<SeedProgramDay>,
)

@Serializable
data class SeedProgramDay(
    val id: String,
    val name: String,
    val exercises: List<SeedProgramExercise>,
)

@Serializable
data class SeedProgramExercise(
    val exerciseId: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val progressionRule: ProgressionRule = ProgressionRule.DOUBLE,
    val supersetGroup: String? = null,
    val targetRpe: Double? = null,
)

internal fun SeedProgram.toEntity(now: Long) = ProgramEntity(
    id = id,
    name = name,
    type = type,
    daysPerWeek = daysPerWeek,
    goalTag = goalTag,
    isTemplate = true,
    sourceTemplateId = null,
    createdAt = now,
    updatedAt = now,
)

internal fun SeedProgram.dayEntities(now: Long) = days.mapIndexed { index, day ->
    ProgramDayEntity(id = day.id, programId = id, dayIndex = index, name = day.name, updatedAt = now)
}

/** Slot ids are derived from the day id and position so re-imports are stable. */
internal fun SeedProgram.slotEntities(now: Long, incrementFor: (String) -> Double) = days.flatMap { day ->
    day.exercises.mapIndexed { position, slot ->
        ProgramExerciseEntity(
            id = "${day.id}#$position",
            programDayId = day.id,
            exerciseId = slot.exerciseId,
            position = position,
            targetSets = slot.sets,
            repMin = slot.repMin,
            repMax = slot.repMax,
            targetRpe = slot.targetRpe,
            restSeconds = slot.restSeconds,
            supersetGroup = slot.supersetGroup,
            progressionRule = slot.progressionRule,
            incrementKg = incrementFor(slot.exerciseId),
            updatedAt = now,
        )
    }
}
