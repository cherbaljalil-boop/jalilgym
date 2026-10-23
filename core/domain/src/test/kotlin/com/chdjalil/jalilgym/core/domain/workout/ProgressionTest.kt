package com.chdjalil.jalilgym.core.domain.workout

import com.chdjalil.jalilgym.core.model.Prescription
import com.chdjalil.jalilgym.core.model.ProgressionOutcome
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.SetType
import com.chdjalil.jalilgym.core.model.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressionTest {

    private val double = Prescription(targetSets = 3, repMin = 8, repMax = 12, restSeconds = 120, incrementKg = 2.5)
    private val linear = double.copy(progressionRule = ProgressionRule.LINEAR, repMin = 5, repMax = 5)

    private var id = 0
    private fun set(weight: Double, reps: Int, type: SetType = SetType.WORKING, completed: Boolean = true, rpe: Double? = null) =
        WorkoutSet("s${id++}", id, type, weight, reps, rpe, null, completed, if (completed) 1L else null)

    @Test
    fun `double progression adds weight when every set hits the top`() {
        val result = applyProgression(double, null, listOf(set(80.0, 12), set(80.0, 12), set(80.0, 12), set(40.0, 10, SetType.WARMUP)))!!
        assertThat(result.outcome).isEqualTo(ProgressionOutcome.INCREASE)
        assertThat(result.fromWeightKg).isEqualTo(80.0)
        assertThat(result.state).isEqualTo(ProgressionState(82.5, 8, 0))
    }

    @Test
    fun `double progression holds weight and aims for one more rep`() {
        val result = applyProgression(double, null, listOf(set(80.0, 12), set(80.0, 10), set(80.0, 9)))!!
        assertThat(result.outcome).isEqualTo(ProgressionOutcome.HOLD)
        assertThat(result.state).isEqualTo(ProgressionState(80.0, 10, 0))
    }

    @Test
    fun `high effort blocks the increase`() {
        val p = double.copy(targetRpe = 8.0)
        val result = applyProgression(p, null, listOf(set(80.0, 12, rpe = 9.5), set(80.0, 12), set(80.0, 12)))!!
        assertThat(result.outcome).isEqualTo(ProgressionOutcome.HOLD)
    }

    @Test
    fun `two misses in a row trigger a deload`() {
        val first = applyProgression(double, null, listOf(set(100.0, 6), set(100.0, 8)))!!
        assertThat(first.outcome).isEqualTo(ProgressionOutcome.MISSED)
        assertThat(first.state.consecutiveFailures).isEqualTo(1)

        val second = applyProgression(double, first.state, listOf(set(100.0, 7)))!!
        assertThat(second.outcome).isEqualTo(ProgressionOutcome.DELOAD)
        assertThat(second.state).isEqualTo(ProgressionState(90.0, 8, 0))
    }

    @Test
    fun `linear progression adds weight after any successful session`() {
        val result = applyProgression(linear, null, listOf(set(100.0, 5), set(100.0, 5), set(100.0, 6)))!!
        assertThat(result.outcome).isEqualTo(ProgressionOutcome.INCREASE)
        assertThat(result.state.nextWeightKg).isEqualTo(102.5)
    }

    @Test
    fun `manual rule and empty sessions produce nothing`() {
        assertThat(applyProgression(double.copy(progressionRule = ProgressionRule.NONE), null, listOf(set(80.0, 12)))).isNull()
        assertThat(applyProgression(double, null, listOf(set(80.0, 12, completed = false)))).isNull()
        assertThat(applyProgression(double, null, listOf(set(40.0, 10, SetType.WARMUP)))).isNull()
    }

    @Test
    fun `volume counts completed non-warm-up sets`() {
        val sets = listOf(set(100.0, 5), set(100.0, 5, completed = false), set(60.0, 8, SetType.WARMUP), set(80.0, 10, SetType.DROP))
        assertThat(volumeKg(sets)).isEqualTo(1300.0)
    }
}
