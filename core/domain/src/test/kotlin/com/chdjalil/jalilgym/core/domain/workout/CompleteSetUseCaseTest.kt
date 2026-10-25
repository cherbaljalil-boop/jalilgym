package com.chdjalil.jalilgym.core.domain.workout

import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.chdjalil.jalilgym.core.testing.repository.FakeRestTimer
import com.chdjalil.jalilgym.core.testing.repository.FakeUserPreferencesRepository
import com.chdjalil.jalilgym.core.testing.repository.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteSetUseCaseTest {

    private val bench = sampleExercise("bench", "Bench")
    private val curl = sampleExercise("curl", "Curl").copy(mechanics = Mechanics.ISOLATION)
    private val workouts = FakeWorkoutRepository(exercises = listOf(bench, curl))
    private val timer = FakeRestTimer()
    private val useCase = CompleteSetUseCase(workouts, FakeUserPreferencesRepository(), timer)
    private val label = { name: String, set: Int, total: Int -> "$name $set/$total" }

    @Test
    fun `completes the next set and starts the default rest`() = runTest {
        val id = workouts.startEmpty("Quick")
        workouts.addExercises(id, listOf("bench", "curl"))

        assertThat(useCase(labelFor = label)).isTrue()

        val session = workouts.observeActiveSession().first()!!
        assertThat(session.exercises[0].sets[0].completed).isTrue()
        val state = timer.state.value!!
        assertThat(state.totalMillis).isEqualTo(180_000L)
        assertThat(state.label).isEqualTo("Bench 2/3")
    }

    @Test
    fun `supersets defer rest until the last exercise`() = runTest {
        val id = workouts.startEmpty("Quick")
        workouts.addExercises(id, listOf("bench", "curl"))
        val session = workouts.observeActiveSession().first()!!
        // Link both exercises as a superset.
        val linked = session.copy(exercises = session.exercises.map { it.copy(supersetGroup = "g") })
        val first = linked.exercises[0]
        val second = linked.exercises[1]

        assertThat(linked.restAfter(first, first.sets[0], 180, 90)).isNull()
        assertThat(linked.restAfter(second, second.sets[0], 180, 90)).isEqualTo(90)
        assertThat(linked.restAfter(second, second.sets[0].copy(type = SetType.WARMUP), 180, 90)).isNull()
    }

    @Test
    fun `nothing to complete without an active workout`() = runTest {
        assertThat(useCase(labelFor = label)).isFalse()
        assertThat(timer.state.value).isNull()
    }
}
