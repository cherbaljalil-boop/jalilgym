package com.chdjalil.jalilgym.core.domain.onboarding

import com.chdjalil.jalilgym.core.model.ExperienceLevel
import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.StarterProgram
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StarterProgramSuggestionTest {

    @Test
    fun `beginners and unknown experience start with full body`() {
        Goal.entries.forEach { goal ->
            assertThat(suggestStarterProgram(goal, ExperienceLevel.BEGINNER)).isEqualTo(StarterProgram.FULL_BODY_3)
        }
        assertThat(suggestStarterProgram(null, null)).isEqualTo(StarterProgram.FULL_BODY_3)
    }

    @Test
    fun `intermediates get PPL, six days when building muscle`() {
        assertThat(suggestStarterProgram(Goal.BUILD_MUSCLE, ExperienceLevel.INTERMEDIATE)).isEqualTo(StarterProgram.PPL_6)
        assertThat(suggestStarterProgram(Goal.GET_STRONGER, ExperienceLevel.INTERMEDIATE)).isEqualTo(StarterProgram.PPL_3)
        assertThat(suggestStarterProgram(null, ExperienceLevel.INTERMEDIATE)).isEqualTo(StarterProgram.PPL_3)
    }

    @Test
    fun `advanced lifters get six-day PPL`() {
        assertThat(suggestStarterProgram(Goal.LOSE_FAT, ExperienceLevel.ADVANCED)).isEqualTo(StarterProgram.PPL_6)
    }
}
