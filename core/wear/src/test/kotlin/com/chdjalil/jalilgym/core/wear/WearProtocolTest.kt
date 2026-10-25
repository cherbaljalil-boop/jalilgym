package com.chdjalil.jalilgym.core.wear

import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.SessionExercise
import com.chdjalil.jalilgym.core.model.SessionStatus
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearProtocolTest {

    private fun set(id: String, position: Int, weight: Double? = null, reps: Int? = null, done: Boolean = false, type: SetType = SetType.WORKING) =
        WorkoutSet(id, position, type, weight, reps, null, null, done, if (done) 1L else null)

    private fun session(vararg exercises: SessionExercise) =
        WorkoutSession("s1", "Push", null, startedAt = 0, endedAt = null, status = SessionStatus.ACTIVE, notes = null, exercises = exercises.toList())

    private val target = Prescription(targetSets = 3, repMin = 6, repMax = 10, restSeconds = 120, incrementKg = 2.5)

    @Test
    fun `no workout means an inactive state`() {
        assertThat(wearWorkoutState(null, null, UnitSystem.METRIC)).isEqualTo(WearWorkoutState())
    }

    @Test
    fun `the next set is suggested from this session, then last time, then the target`() {
        val bench = SessionExercise(
            "e1", sampleExercise("bench", "Bench Press"), 0, null, "p1", target,
            sets = listOf(set("a", 0, 80.0, 8, done = true), set("b", 1), set("c", 2)),
            previous = listOf(set("x", 0, 77.5, 9, true), set("y", 1, 77.5, 8, true), set("z", 2, 77.5, 7, true)),
        )
        val state = wearWorkoutState(session(bench), RestTimerState(0, 90_000, "Bench Press · set 2 of 3"), UnitSystem.IMPERIAL)

        assertThat(state.active).isTrue()
        assertThat(state.completedSets).isEqualTo(1)
        assertThat(state.totalSets).isEqualTo(3)
        assertThat(state.restEndsAt).isEqualTo(90_000)
        assertThat(state.units).isEqualTo(UnitSystem.IMPERIAL)
        val next = state.next!!
        assertThat(next.setId).isEqualTo("b")
        assertThat(next.number).isEqualTo(2)
        assertThat(next.count).isEqualTo(3)
        // Carries on from the set just done rather than last session's.
        assertThat(next.weightKg).isEqualTo(80.0)
        assertThat(next.reps).isEqualTo(8)
    }

    @Test
    fun `first set falls back to last time and then to the program target`() {
        val fromHistory = SessionExercise(
            "e1", sampleExercise("row"), 0, null, null, target,
            sets = listOf(set("a", 0)), previous = listOf(set("x", 0, 60.0, 10, true)),
        )
        assertThat(wearWorkoutState(session(fromHistory), null, UnitSystem.METRIC).next!!.weightKg).isEqualTo(60.0)

        val fresh = SessionExercise("e2", sampleExercise("squat"), 0, null, null, target, sets = listOf(set("a", 0, type = SetType.WARMUP)))
        val next = wearWorkoutState(session(fresh), null, UnitSystem.METRIC).next!!
        assertThat(next.weightKg).isNull()
        assertThat(next.reps).isEqualTo(6)
        assertThat(next.isWarmup).isTrue()
    }

    @Test
    fun `everything done leaves no next set, and the JSON round-trips`() {
        val done = SessionExercise("e1", sampleExercise("curl"), 0, null, null, null, sets = listOf(set("a", 0, 12.0, 12, done = true)))
        val state = wearWorkoutState(session(done), null, UnitSystem.METRIC)
        assertThat(state.next).isNull()
        assertThat(state.completedSets).isEqualTo(1)

        val text = WearJson.encodeToString(WearWorkoutState.serializer(), state)
        assertThat(WearJson.decodeFromString(WearWorkoutState.serializer(), text)).isEqualTo(state)
        val completion = WearSetCompletion("b", 82.5, 8)
        val encoded = WearJson.encodeToString(WearSetCompletion.serializer(), completion)
        assertThat(WearJson.decodeFromString(WearSetCompletion.serializer(), encoded)).isEqualTo(completion)
    }
}
