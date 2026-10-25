package com.chdjalil.jalilgym.feature.programs

import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Program
import com.chdjalil.jalilgym.core.model.ProgramDay
import com.chdjalil.jalilgym.core.model.ProgramType
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeProgramRepository

internal val squat = sampleExercise("squat", "Squat")
internal val curl = sampleExercise("curl", "Curl").copy(mechanics = Mechanics.ISOLATION)
internal val row = sampleExercise("row", "Row")

internal val pplTemplate = Program(
    id = "tpl-ppl-3",
    name = "Push / Pull / Legs",
    type = ProgramType.PPL,
    daysPerWeek = 3,
    isTemplate = true,
    sourceTemplateId = null,
    days = listOf(
        ProgramDay("t-push", "tpl-ppl-3", 0, "Push", emptyList()),
        ProgramDay("t-pull", "tpl-ppl-3", 1, "Pull", emptyList()),
        ProgramDay("t-legs", "tpl-ppl-3", 2, "Legs", emptyList()),
    ),
)

internal fun programRepository() = FakeProgramRepository(exercises = listOf(squat, curl, row), templates = listOf(pplTemplate))
