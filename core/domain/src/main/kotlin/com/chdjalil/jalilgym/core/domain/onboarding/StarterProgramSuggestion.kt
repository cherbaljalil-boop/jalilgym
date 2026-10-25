package com.chdjalil.jalilgym.core.domain.onboarding

import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram

/** Suggests a first program from onboarding answers (docs/BLUEPRINT.md §3.1). */
fun suggestStarterProgram(goal: Goal?, experience: ExperienceLevel?): StarterProgram = when (experience) {
    null, ExperienceLevel.BEGINNER -> StarterProgram.FULL_BODY_3
    ExperienceLevel.INTERMEDIATE ->
        if (goal == Goal.BUILD_MUSCLE) StarterProgram.PPL_6 else StarterProgram.PPL_3
    ExperienceLevel.ADVANCED -> StarterProgram.PPL_6
}
