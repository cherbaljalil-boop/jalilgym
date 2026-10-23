package com.chdjalil.jalilgym.core.domain.units

import com.chdjalil.jalilgym.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightUnitsTest {

    @Test
    fun `formats with at most one decimal`() {
        assertThat(formatNumber(82.5)).isEqualTo("82.5")
        assertThat(formatNumber(80.0)).isEqualTo("80")
        assertThat(formatNumber(181.88)).isEqualTo("181.9")
        assertThat(formatNumber(0.04)).isEqualTo("0")
    }

    @Test
    fun `imperial input round-trips through kg`() {
        val kg = UnitSystem.IMPERIAL.parseWeightToKg("225")!!
        assertThat(kg).isWithin(1e-6).of(102.058283)
        assertThat(UnitSystem.IMPERIAL.formatWeightValue(kg)).isEqualTo("225")
    }

    @Test
    fun `parsing accepts commas and rejects junk`() {
        assertThat(UnitSystem.METRIC.parseWeightToKg("82,5")).isEqualTo(82.5)
        assertThat(UnitSystem.METRIC.parseWeightToKg("abc")).isNull()
        assertThat(UnitSystem.METRIC.parseWeightToKg("-5")).isNull()
    }
}
