package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.WaterDay
import com.chdjalil.jalilgym.core.model.WaterEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun observeDay(date: LocalDate): Flow<WaterDay>

    /** Entries from [since] onwards, oldest first. */
    fun observeEntriesSince(since: LocalDate): Flow<List<WaterEntry>>

    suspend fun add(date: LocalDate, ml: Int)

    /** Removes the most recent drink on [date], if any. */
    suspend fun undoLast(date: LocalDate)
}
