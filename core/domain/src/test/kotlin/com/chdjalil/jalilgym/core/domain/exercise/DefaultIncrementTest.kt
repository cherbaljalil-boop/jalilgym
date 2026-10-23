package com.chdjalil.jalilgym.core.domain.exercise

import com.chdjalil.jalilgym.core.model.Equipment
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.Muscle
import com.chdjalil.jalilgym.core.model.TrackingType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultIncrementTest {

    @Test
    fun `lower-body barbell compounds jump 5 kg, upper-body 2_5 kg`() {
        assertThat(inc(Equipment.BARBELL, Mechanics.COMPOUND, Muscle.QUADS)).isEqualTo(5.0)
        assertThat(inc(Equipment.BARBELL, Mechanics.COMPOUND, Muscle.CHEST)).isEqualTo(2.5)
    }

    @Test
    fun `dumbbells use small jumps`() {
        assertThat(inc(Equipment.DUMBBELL, Mechanics.COMPOUND, Muscle.CHEST)).isEqualTo(2.0)
        assertThat(inc(Equipment.DUMBBELL, Mechanics.ISOLATION, Muscle.BICEPS)).isEqualTo(1.0)
    }

    @Test
    fun `timed and distance work has no weight jump`() {
        assertThat(inc(Equipment.BODYWEIGHT, Mechanics.ISOLATION, Muscle.ABS, TrackingType.TIME)).isEqualTo(0.0)
    }

    private fun inc(e: Equipment, m: Mechanics, muscle: Muscle, t: TrackingType = TrackingType.WEIGHT_REPS) =
        defaultIncrementKg(e, m, listOf(muscle), t)
}
