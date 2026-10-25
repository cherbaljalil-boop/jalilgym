package com.chdjalil.jalilgym.core.domain.hydration

import com.chdjalil.jalilgym.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HydrationTest {

    @Test
    fun `automatic goal is about 35 ml per kg in 250 ml steps`() {
        assertThat(automaticWaterGoalMl(80.0)).isEqualTo(2_750) // 2,800 → nearest 250
        assertThat(automaticWaterGoalMl(70.0)).isEqualTo(2_500) // 2,450
        assertThat(automaticWaterGoalMl(57.0)).isEqualTo(2_000) // 1,995
    }

    @Test
    fun `automatic goal stays between 1,5 and 4 litres`() {
        assertThat(automaticWaterGoalMl(30.0)).isEqualTo(1_500)
        assertThat(automaticWaterGoalMl(160.0)).isEqualTo(4_000)
    }

    @Test
    fun `without a weight the default goal is used`() {
        assertThat(automaticWaterGoalMl(null)).isEqualTo(DEFAULT_WATER_GOAL_ML)
        assertThat(automaticWaterGoalMl(0.0)).isEqualTo(DEFAULT_WATER_GOAL_ML)
    }

    @Test
    fun `a goal the user set wins`() {
        assertThat(waterGoalMl(3_200, 60.0)).isEqualTo(3_200)
        assertThat(waterGoalMl(null, 60.0)).isEqualTo(2_000)
    }

    @Test
    fun `quick amounts match the unit system`() {
        assertThat(quickWaterAmountsMl(UnitSystem.METRIC)).containsExactly(250, 500).inOrder()
        assertThat(quickWaterAmountsMl(UnitSystem.IMPERIAL)).containsExactly(237, 473).inOrder()
        assertThat(mlToFlOz(473)).isWithin(0.05).of(16.0)
    }
}
