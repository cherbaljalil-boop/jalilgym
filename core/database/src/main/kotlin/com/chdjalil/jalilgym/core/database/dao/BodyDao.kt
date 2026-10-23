package com.chdjalil.jalilgym.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chdjalil.jalilgym.core.database.entity.BodyMeasurementEntity
import com.chdjalil.jalilgym.core.database.entity.BodyWeightEntity
import com.chdjalil.jalilgym.core.database.entity.ProgressPhotoEntity
import com.chdjalil.jalilgym.core.model.MeasurementType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyDao {

    @Query("SELECT * FROM body_weights WHERE deletedAt IS NULL ORDER BY date")
    fun observeWeights(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights WHERE date = :date AND deletedAt IS NULL LIMIT 1")
    suspend fun weightOn(date: LocalDate): BodyWeightEntity?

    @Query("SELECT * FROM body_weights WHERE deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    fun observeLatestWeight(): Flow<BodyWeightEntity?>

    @Upsert
    suspend fun upsertWeight(weight: BodyWeightEntity)

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteWeight(id: String)

    @Query("SELECT * FROM body_measurements WHERE deletedAt IS NULL ORDER BY date DESC, type")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date = :date AND type = :type AND deletedAt IS NULL LIMIT 1")
    suspend fun measurementOn(date: LocalDate, type: MeasurementType): BodyMeasurementEntity?

    @Upsert
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteMeasurement(id: String)

    @Query("SELECT * FROM progress_photos WHERE deletedAt IS NULL ORDER BY date DESC, updatedAt DESC")
    fun observePhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photos WHERE id = :id")
    suspend fun getPhoto(id: String): ProgressPhotoEntity?

    @Upsert
    suspend fun upsertPhoto(photo: ProgressPhotoEntity)

    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Query("SELECT COUNT(*) FROM progress_photos WHERE deletedAt IS NULL")
    fun observePhotoCount(): Flow<Int>
}
