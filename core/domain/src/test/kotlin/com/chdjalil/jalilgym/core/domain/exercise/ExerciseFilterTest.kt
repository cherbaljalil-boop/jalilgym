package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.Difficulty
import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Exercise
import com.chdjalil.jalilgym.core.model.Force
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseFilterTest {

    private val bench = exercise(
        "bench", "Barbell Bench Press", aliases = listOf("Flat Bench"),
        primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS),
    )
    private val pullUp = exercise(
        "pull-up", "Pull-Up", equipment = Equipment.BODYWEIGHT, difficulty = Difficulty.INTERMEDIATE,
        primary = listOf(Muscle.LATS), secondary = listOf(Muscle.BICEPS),
    )
    private val curl = exercise(
        "curl", "Dumbbell Biceps Curl", equipment = Equipment.DUMBBELL, mechanics = Mechanics.ISOLATION,
        primary = listOf(Muscle.BICEPS),
    )
    private val all = listOf(bench, pullUp, curl)

    @Test
    fun `empty filter returns everything`() {
        assertThat(all.filteredBy(ExerciseFilter())).containsExactlyElementsIn(all).inOrder()
    }

    @Test
    fun `query matches name and aliases, case and punctuation insensitive`() {
        assertThat(all.filteredBy(ExerciseFilter(query = "flat"))).containsExactly(bench)
        assertThat(all.filteredBy(ExerciseFilter(query = "PULL UP"))).containsExactly(pullUp)
        assertThat(all.filteredBy(ExerciseFilter(query = "  dumbbell   curl "))).containsExactly(curl)
        assertThat(all.filteredBy(ExerciseFilter(query = "curl squat"))).isEmpty()
    }

    @Test
    fun `muscle filter uses primary muscles unless secondary is included`() {
        val biceps = ExerciseFilter(muscles = setOf(Muscle.BICEPS))
        assertThat(all.filteredBy(biceps)).containsExactly(curl)
        assertThat(all.filteredBy(biceps.copy(includeSecondary = true))).containsExactly(pullUp, curl).inOrder()
    }

    @Test
    fun `options within a group are OR'ed and groups are AND'ed`() {
        val filter = ExerciseFilter(
            muscles = setOf(Muscle.CHEST, Muscle.BICEPS),
            equipment = setOf(Equipment.DUMBBELL),
        )
        assertThat(all.filteredBy(filter)).containsExactly(curl)
    }

    @Test
    fun `mechanics and difficulty filters`() {
        assertThat(all.filteredBy(ExerciseFilter(mechanics = setOf(Mechanics.ISOLATION)))).containsExactly(curl)
        assertThat(all.filteredBy(ExerciseFilter(difficulty = setOf(Difficulty.INTERMEDIATE)))).containsExactly(pullUp)
    }

    @Test
    fun `active filter count ignores the query`() {
        assertThat(ExerciseFilter(query = "x").activeFilterCount).isEqualTo(0)
        assertThat(
            ExerciseFilter(muscles = setOf(Muscle.ABS), equipment = setOf(Equipment.CABLE)).activeFilterCount,
        ).isEqualTo(2)
    }

    private fun exercise(
        id: String,
        name: String,
        aliases: List<String> = emptyList(),
        equipment: Equipment = Equipment.BARBELL,
        mechanics: Mechanics = Mechanics.COMPOUND,
        difficulty: Difficulty = Difficulty.BEGINNER,
        primary: List<Muscle>,
        secondary: List<Muscle> = emptyList(),
    ) = Exercise(
        id = id,
        name = name,
        aliases = aliases,
        instructions = listOf("Lift."),
        equipment = equipment,
        mechanics = mechanics,
        force = Force.PUSH,
        difficulty = difficulty,
        trackingType = TrackingType.WEIGHT_REPS,
        defaultIncrementKg = 2.5,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        isBuiltIn = true,
    )
}
