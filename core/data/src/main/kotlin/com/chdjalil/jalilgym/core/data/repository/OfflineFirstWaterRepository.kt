package com.chdjalil.jalilgym.core.data.repository

import com.chdjalil.jalilgym.core.database.dao.WaterDao
import com.chdjalil.jalilgym.core.database.entity.WaterLogEntity
import com.chdjalil.jalilgym.core.domain.repository.WaterRepository
import com.chdjalil.jalilgym.core.model.WaterDay
import com.chdjalil.jalilgym.core.model.WaterEntry
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class OfflineFirstWaterRepository @Inject constructor(
    private val waterDao: WaterDao,
) : WaterRepository {

    override fun observeDay(date: LocalDate): Flow<WaterDay> =
        waterDao.observeDay(date).map { list -> WaterDay(date, list.map { it.toModel() }) }

    override fun observeEntriesSince(since: LocalDate): Flow<List<WaterEntry>> =
        waterDao.observeSince(since).map { list -> list.map { it.toModel() } }

    override suspend fun add(date: LocalDate, ml: Int) {
        require(ml > 0) { "A drink must have a volume" }
        val now = System.currentTimeMillis()
        waterDao.insert(WaterLogEntity(UUID.randomUUID().toString(), date, ml, loggedAt = now, updatedAt = now))
    }

    override suspend fun undoLast(date: LocalDate) = waterDao.deleteLatest(date)
}

private fun WaterLogEntity.toModel() = WaterEntry(id, date, ml, loggedAt)
