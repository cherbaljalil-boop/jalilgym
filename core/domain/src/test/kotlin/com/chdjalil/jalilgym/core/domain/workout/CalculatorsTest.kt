package com.chdjalil.jalilgym.core.domain.workout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorsTest {

    private val kgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    @Test
    fun `epley and brzycki estimates`() {
        assertThat(estimateOneRepMax(100.0, 5)!!).isWithin(1e-9).of(116.6666666667)
        assertThat(estimateOneRepMax(100.0, 5, OneRepMaxFormula.BRZYCKI)!!).isWithin(1e-9).of(112.5)
        assertThat(estimateOneRepMax(140.0, 1)).isEqualTo(140.0)
        assertThat(estimateOneRepMax(100.0, 0)).isNull()
        assertThat(estimateOneRepMax(0.0, 5)).isNull()
        assertThat(estimateOneRepMax(50.0, 40, OneRepMaxFormula.BRZYCKI)).isNull()
    }

    @Test
    fun `percent table covers 50 to 95 percent`() {
        val table = percentTable(200.0)
        assertThat(table.first()).isEqualTo(50 to 100.0)
        assertThat(table.last()).isEqualTo(95 to 190.0)
        assertThat(table).hasSize(10)
    }

    @Test
    fun `plates are loaded largest first per side`() {
        val load = calculatePlates(102.5, bar = 20.0, plates = kgPlates)
        assertThat(load.perSide).containsExactly(25.0, 15.0, 1.25).inOrder()
        assertThat(load.achieved).isEqualTo(102.5)
    }

    @Test
    fun `unloadable weights fall short and report the difference`() {
        val load = calculatePlates(101.0, bar = 20.0, plates = kgPlates)
        assertThat(load.achieved).isEqualTo(100.0)
        assertThat(load.shortfall(101.0)).isWithin(1e-9).of(1.0)
        assertThat(calculatePlates(15.0, bar = 20.0, plates = kgPlates).perSide).isEmpty()
    }

    @Test
    fun `nearest loadable rounds to the closer option`() {
        assertThat(nearestLoadable(101.0, 20.0, kgPlates)).isEqualTo(100.0)
        assertThat(nearestLoadable(101.9, 20.0, kgPlates)).isEqualTo(102.5)
    }

    @Test
    fun `warm-ups ramp up from the empty bar`() {
        val warmups = generateWarmups(100.0, bar = 20.0, plates = kgPlates)
        assertThat(warmups).containsExactly(
            WarmupSet(20.0, 10),
            WarmupSet(40.0, 8),
            WarmupSet(60.0, 5),
            WarmupSet(75.0, 3),
            WarmupSet(85.0, 1),
        ).inOrder()
    }

    @Test
    fun `light working weights skip duplicate and bar-level steps`() {
        val warmups = generateWarmups(30.0, bar = 20.0, plates = kgPlates)
        assertThat(warmups.first()).isEqualTo(WarmupSet(20.0, 10))
        assertThat(warmups.map { it.weight }).isInStrictOrder()
        assertThat(warmups.all { it.weight < 30.0 }).isTrue()
        assertThat(generateWarmups(20.0, bar = 20.0, plates = kgPlates)).isEmpty()
    }

    @Test
    fun `round to step`() {
        assertThat(roundToStep(74.25, 2.5)).isEqualTo(75.0)
        assertThat(roundToStep(73.6, 2.5)).isEqualTo(72.5)
        assertThat(roundToStep(73.6, 0.0)).isEqualTo(73.6)
    }
}
