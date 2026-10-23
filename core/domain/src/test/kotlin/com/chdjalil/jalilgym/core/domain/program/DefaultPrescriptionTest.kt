package com.chdjalil.jalilgym.core.domain.program

import com.chdjalil.jalilgym.core.model.Goal
import com.chdjalil.jalilgym.core.model.Mechanics
import com.chdjalil.jalilgym.core.model.ProgressionRule
import com.chdjalil.jalilgym.core.model.TrackingType
import com.chdjalil.jalilgym.core.testing.data.sampleExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DefaultPrescriptionTest {

    private val compound = sampleExercise("bench")
    private val isolation = sampleExercise("curl").copy(mechanics = Mechanics.ISOLATION, defaultIncrementKg = 1.0)

    @Test
    fun `strength goal uses low reps on compounds`() {
        val p = defaultPrescription(compound, Goal.GET_STRONGER, 180, 90)
        assertThat(p.repMin to p.repMax).isEqualTo(4 to 6)
        assertThat(p.restSeconds).isEqualTo(180)
        assertThat(p.progressionRule).isEqualTo(ProgressionRule.DOUBLE)
        assertThat(p.incrementKg).isEqualTo(2.5)
    }

    @Test
    fun `isolation work gets higher reps and shorter rest`() {
        val p = defaultPrescription(isolation, Goal.BUILD_MUSCLE, 180, 90)
        assertThat(p.repMin to p.repMax).isEqualTo(10 to 15)
        assertThat(p.restSeconds).isEqualTo(90)
        assertThat(p.incrementKg).isEqualTo(1.0)
    }

    @Test
    fun `timed exercises are prescribed in seconds without progression`() {
        val plank = isolation.copy(trackingType = TrackingType.TIME)
        val p = defaultPrescription(plank, null, 180, 90)
        assertThat(p.repMin to p.repMax).isEqualTo(30 to 60)
        assertThat(p.progressionRule).isEqualTo(ProgressionRule.NONE)
    }

    @Test
    fun `rotation wraps around`() {
        assertThat(nextRotationIndex(0, 3)).isEqualTo(1)
        assertThat(nextRotationIndex(2, 3)).isEqualTo(0)
        assertThat(nextRotationIndex(5, 0)).isEqualTo(0)
    }
}
