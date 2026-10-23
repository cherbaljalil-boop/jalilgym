package com.chdjalil.jalilgym.core.testing.repository

import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.model.BodyMeasurement
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.ProgressPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeBodyRepository : BodyRepository {
    val weights = MutableStateFlow<List<BodyWeight>>(emptyList())
    val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    val photos = MutableStateFlow<List<ProgressPhoto>>(emptyList())
    private var nextId = 1

    override fun observeWeights(): Flow<List<BodyWeight>> = weights

    override suspend fun logWeight(date: LocalDate, weightKg: Double) = weights.update { list ->
        (list.filterNot { it.date == date } + BodyWeight("w${nextId++}", date, weightKg)).sortedBy { it.date }
    }

    override suspend fun deleteWeight(id: String) = weights.update { list -> list.filterNot { it.id == id } }

    override fun observeMeasurements(): Flow<List<BodyMeasurement>> = measurements

    override suspend fun logMeasurement(date: LocalDate, type: MeasurementType, value: Double) = measurements.update { list ->
        (list.filterNot { it.date == date && it.type == type } + BodyMeasurement("m${nextId++}", date, type, value))
            .sortedByDescending { it.date }
    }

    override suspend fun deleteMeasurement(id: String) = measurements.update { list -> list.filterNot { it.id == id } }

    override fun observePhotos(): Flow<List<ProgressPhoto>> = photos

    override suspend fun addPhoto(sourceUri: String, pose: PhotoPose, date: LocalDate) = photos.update { list ->
        (list + ProgressPhoto("p${nextId++}", date, pose, sourceUri, weights.value.lastOrNull()?.weightKg))
            .sortedByDescending { it.date }
    }

    override suspend fun deletePhoto(id: String) = photos.update { list -> list.filterNot { it.id == id } }
}
