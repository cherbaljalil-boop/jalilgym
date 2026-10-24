package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chdjalil.jalilgym.core.database.entity.WaterLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_logs WHERE date = :date AND deletedAt IS NULL ORDER BY loggedAt")
    fun observeDay(date: LocalDate): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE date >= :since AND deletedAt IS NULL ORDER BY loggedAt")
    fun observeSince(since: LocalDate): Flow<List<WaterLogEntity>>

    @Insert
    suspend fun insert(entry: WaterLogEntity)

    @Query(
        """
        DELETE FROM water_logs WHERE id =
            (SELECT id FROM water_logs WHERE date = :date AND deletedAt IS NULL ORDER BY loggedAt DESC LIMIT 1)
        """,
    )
    suspend fun deleteLatest(date: LocalDate)
}
