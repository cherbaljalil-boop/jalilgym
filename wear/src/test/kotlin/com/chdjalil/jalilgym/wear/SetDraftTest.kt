package com.chdjalil.jalilgym.wear

import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.model.UnitSystem
import com.chdjalil.jalilgym.core.wear.WearSet
import com.chdjalil.jalilgym.core.wear.WearSetCompletion
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetDraftTest {

    private fun set(weightKg: Double?, reps: Int?, tracking: TrackingType = TrackingType.WEIGHT_REPS) =
        WearSet("s1", "Bench Press", 2, 4, tracking, isWarmup = false, weightKg = weightKg, reps = reps, incrementKg = 2.5)

    @Test
    fun `metric steps are 2,5 kg and one rep`() {
        val draft = SetDraft.from(set(80.0, 8), UnitSystem.METRIC).changeWeight(1).changeReps(-1)
        assertThat(draft.weight).isEqualTo(82.5)
        assertThat(draft.completion()).isEqualTo(WearSetCompletion("s1", 82.5, 7))
    }

    @Test
    fun `imperial shows pounds and an untouched weight goes back unchanged`() {
        val draft = SetDraft.from(set(80.0, 8), UnitSystem.IMPERIAL)
        assertThat(draft.weight).isEqualTo(176.4)
        assertThat(draft.completion().weightKg).isEqualTo(80.0)

        val heavier = draft.changeWeight(1)
        assertThat(heavier.weight).isEqualTo(181.4)
        assertThat(heavier.completion().weightKg!!).isWithin(0.01).of(82.28)
    }

    @Test
    fun `values never go below zero and timed sets step by five seconds`() {
        val empty = SetDraft.from(set(null, null), UnitSystem.METRIC).changeWeight(-3).changeReps(-2)
        assertThat(empty.weight).isEqualTo(0.0)
        assertThat(empty.reps).isEqualTo(0)

        val plank = SetDraft.from(set(null, 45, TrackingType.TIME), UnitSystem.METRIC)
        assertThat(plank.tracksWeight).isFalse()
        assertThat(plank.changeReps(1).reps).isEqualTo(50)
        assertThat(plank.changeWeight(1)).isEqualTo(plank)
        assertThat(plank.completion()).isEqualTo(WearSetCompletion("s1", null, 45))
    }
}
