package com.chdjalil.jalilgym.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.PhotoPose
import java.time.LocalDate

@Entity(tableName = "body_weights", indices = [Index("date")])
data class BodyWeightEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val weightKg: Double,
    val healthConnectId: String?,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "body_measurements", indices = [Index("date"), Index("type")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val type: MeasurementType,
    /** cm for circumferences, percent for BODY_FAT. */
    val value: Double,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "progress_photos", indices = [Index("date")])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val pose: PhotoPose,
    /** App-internal file path; photos are never written to shared storage. */
    val filePath: String,
    val weightKgAtTime: Double?,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Achievement definitions live in code; only unlocks are stored. */
@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
    val sourceRefId: String?,
)
