package com.chdjalil.jalilgym.core.model

import java.time.LocalDate

/** One drink logged in the water tracker. */
data class WaterEntry(
    val id: String,
    val date: LocalDate,
    val ml: Int,
    /** Epoch millis. */
    val loggedAt: Long,
)

data class WaterDay(
    val date: LocalDate,
    val entries: List<WaterEntry> = emptyList(),
) {
    val totalMl: Int get() = entries.sumOf { it.ml }
}
