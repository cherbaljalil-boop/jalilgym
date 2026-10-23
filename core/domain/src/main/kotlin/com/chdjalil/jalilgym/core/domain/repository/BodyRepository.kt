package com.chdjalil.jalilgym.core.domain.repository

import com.chdjalil.jalilgym.core.model.BodyMeasurement
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.ProgressPhoto
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface BodyRepository {
    /** Oldest first. */
    fun observeWeights(): Flow<List<BodyWeight>>

    /** One weight per day: logging again on the same date replaces it. */
    suspend fun logWeight(date: LocalDate, weightKg: Double)

    suspend fun deleteWeight(id: String)

    /** Newest first. */
    fun observeMeasurements(): Flow<List<BodyMeasurement>>

    /** One value per type per day. */
    suspend fun logMeasurement(date: LocalDate, type: MeasurementType, value: Double)

    suspend fun deleteMeasurement(id: String)

    /** Newest first. */
    fun observePhotos(): Flow<List<ProgressPhoto>>

    /** Copies the picked/taken image into private storage. */
    suspend fun addPhoto(sourceUri: String, pose: PhotoPose, date: LocalDate)

    suspend fun deletePhoto(id: String)
}
