package com.chdjalil.jalilgym.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "water_logs", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val ml: Int,
    val loggedAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
