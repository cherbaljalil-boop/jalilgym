package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.WaterRepository
import com.chdjalil.jalilgym.core.model.WaterDay
import com.chdjalil.jalilgym.core.model.WaterEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWaterRepository : WaterRepository {
    val entries = MutableStateFlow<List<WaterEntry>>(emptyList())
    private var nextId = 0
    private var clock = 0L

    override fun observeDay(date: LocalDate): Flow<WaterDay> = entries.map { all -> WaterDay(date, all.filter { it.date == date }) }

    override fun observeEntriesSince(since: LocalDate): Flow<List<WaterEntry>> = entries.map { all -> all.filter { it.date >= since } }

    override suspend fun add(date: LocalDate, ml: Int) {
        entries.update { it + WaterEntry("w${nextId++}", date, ml, ++clock) }
    }

    override suspend fun undoLast(date: LocalDate) {
        entries.update { all ->
            val last = all.filter { it.date == date }.maxByOrNull { it.loggedAt } ?: return@update all
            all - last
        }
    }
}
