package com.chdjalil.jalilgym.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitsTest {

    @Test
    fun `kg to lb uses the international pound`() {
        assertThat(Units.kgToLb(100.0)).isWithin(1e-6).of(220.46226218)
    }

    @Test
    fun `lb to kg round-trips`() {
        assertThat(Units.lbToKg(Units.kgToLb(82.5))).isWithin(1e-9).of(82.5)
    }

    @Test
    fun `inches to cm`() {
        assertThat(Units.inchesToCm(10.0)).isWithin(1e-9).of(25.4)
        assertThat(Units.cmToInches(25.4)).isWithin(1e-9).of(10.0)
    }
}
