package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.ExerciseDraft
import com.chdjalil.jalilgym.core.model.Muscle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseDraftValidationTest {

    private val valid = ExerciseDraft(name = "Landmine Press", primaryMuscles = setOf(Muscle.FRONT_DELTS))

    @Test
    fun `valid draft has no errors`() {
        assertThat(valid.validate()).isEmpty()
    }

    @Test
    fun `name is required and limited in length`() {
        assertThat(valid.copy(name = "   ").validate()).containsExactly(ExerciseDraftError.NAME_BLANK)
        assertThat(valid.copy(name = "x".repeat(EXERCISE_NAME_MAX_LENGTH + 1)).validate())
            .containsExactly(ExerciseDraftError.NAME_TOO_LONG)
    }

    @Test
    fun `needs a primary muscle that is not also secondary`() {
        assertThat(valid.copy(primaryMuscles = emptySet()).validate())
            .containsExactly(ExerciseDraftError.NO_PRIMARY_MUSCLE)
        assertThat(valid.copy(secondaryMuscles = setOf(Muscle.FRONT_DELTS)).validate())
            .containsExactly(ExerciseDraftError.MUSCLE_IN_BOTH_ROLES)
    }
}
