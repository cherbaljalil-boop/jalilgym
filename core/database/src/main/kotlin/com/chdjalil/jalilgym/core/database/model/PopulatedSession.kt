package com.chdjalil.jalilgym.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.chdjalil.jalilgym.core.database.entity.ExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.ProgramExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.SessionExerciseEntity
import com.chdjalil.jalilgym.core.database.entity.SetLogEntity
import com.chdjalil.jalilgym.core.database.entity.WorkoutSessionEntity
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet

data class PopulatedSessionExercise(
    @Embedded val entry: SessionExerciseEntity,
    @Relation(entity = ExerciseEntity::class, parentColumn = "exerciseId", entityColumn = "id")
    val exercise: PopulatedExercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetLogEntity>,
    @Relation(parentColumn = "programExerciseId", entityColumn = "id")
    val slot: ProgramExerciseEntity?,
)

data class PopulatedSession(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<PopulatedSessionExercise>,
)

fun SetLogEntity.asExternalModel() = WorkoutSet(
    id = id,
    position = position,
    type = setType,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    rir = rir,
    completed = completed,
    completedAt = completedAt,
)

fun ProgramExerciseEntity.prescription() = Prescription(
    targetSets = targetSets,
    repMin = repMin,
    repMax = repMax,
    targetRpe = targetRpe,
    restSeconds = restSeconds,
    progressionRule = progressionRule,
    incrementKg = incrementKg,
    deloadAfterFailures = deloadAfterFailures,
    deloadPercent = deloadPercent,
)

fun PopulatedSessionExercise.asExternalModel(previous: List<WorkoutSet> = emptyList()) = SessionExercise(
    id = entry.id,
    exercise = exercise.asExternalModel(),
    position = entry.position,
    supersetGroup = entry.supersetGroup,
    programExerciseId = entry.programExerciseId,
    target = slot?.prescription(),
    sets = sets.sortedBy { it.position }.map { it.asExternalModel() },
    previous = previous,
)

fun PopulatedSession.asExternalModel(previousFor: (String) -> List<WorkoutSet> = { emptyList() }) = WorkoutSession(
    id = session.id,
    name = session.name,
    programDayId = session.programDayId,
    startedAt = session.startedAt,
    endedAt = session.endedAt,
    status = session.status,
    notes = session.notes,
    exercises = exercises.sortedBy { it.entry.position }.map { it.asExternalModel(previousFor(it.entry.exerciseId)) },
)
