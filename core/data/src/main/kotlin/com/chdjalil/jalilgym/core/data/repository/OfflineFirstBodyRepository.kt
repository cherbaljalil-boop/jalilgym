package com.chdjalil.jalilgym.core.data.repository

import android.content.Context
import com.chdjalil.jalilgym.core.common.Dispatcher
import com.chdjalil.jalilgym.core.common.JgDispatchers
import com.chdjalil.jalilgym.core.data.media.importDownscaledJpeg
import com.chdjalil.jalilgym.core.database.dao.BodyDao
import com.chdjalil.jalilgym.core.database.entity.BodyMeasurementEntity
import com.chdjalil.jalilgym.core.database.entity.BodyWeightEntity
import com.chdjalil.jalilgym.core.database.entity.ProgressPhotoEntity
import com.chdjalil.jalilgym.core.domain.repository.BodyRepository
import com.chdjalil.jalilgym.core.model.BodyMeasurement
import com.chdjalil.jalilgym.core.model.BodyWeight
import com.chdjalil.jalilgym.core.model.MeasurementType
import com.chdjalil.jalilgym.core.model.PhotoPose
import com.chdjalil.jalilgym.core.model.ProgressPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstBodyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bodyDao: BodyDao,
    @Dispatcher(JgDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : BodyRepository {

    /** Excluded from backups by the app's backup and data-extraction rules. */
    private val photoDirectory: File get() = File(context.filesDir, PHOTO_DIRECTORY)

    private fun now() = System.currentTimeMillis()

    override fun observeWeights(): Flow<List<BodyWeight>> =
        bodyDao.observeWeights().map { list -> list.map { BodyWeight(it.id, it.date, it.weightKg) } }

    override suspend fun logWeight(date: LocalDate, weightKg: Double) {
        val existing = bodyDao.weightOn(date)
        bodyDao.upsertWeight(
            BodyWeightEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                date = date,
                weightKg = weightKg,
                healthConnectId = existing?.healthConnectId,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun deleteWeight(id: String) = bodyDao.deleteWeight(id)

    override fun observeMeasurements(): Flow<List<BodyMeasurement>> = bodyDao.observeMeasurements().map { list ->
        list.map { BodyMeasurement(it.id, it.date, it.type, it.value) }
    }

    override suspend fun logMeasurement(date: LocalDate, type: MeasurementType, value: Double) {
        val existing = bodyDao.measurementOn(date, type)
        bodyDao.upsertMeasurement(
            BodyMeasurementEntity(existing?.id ?: UUID.randomUUID().toString(), date, type, value, updatedAt = now()),
        )
    }

    override suspend fun deleteMeasurement(id: String) = bodyDao.deleteMeasurement(id)

    override fun observePhotos(): Flow<List<ProgressPhoto>> = bodyDao.observePhotos().map { list ->
        list.map { ProgressPhoto(it.id, it.date, it.pose, it.filePath, it.weightKgAtTime) }
    }

    override suspend fun addPhoto(sourceUri: String, pose: PhotoPose, date: LocalDate) {
        val file = withContext(ioDispatcher) { importDownscaledJpeg(context, sourceUri, photoDirectory, MAX_PHOTO_EDGE_PX) }
        val weight = bodyDao.observeLatestWeight().first()?.weightKg
        bodyDao.upsertPhoto(
            ProgressPhotoEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                pose = pose,
                filePath = file.absolutePath,
                weightKgAtTime = weight,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun deletePhoto(id: String) {
        val photo = bodyDao.getPhoto(id) ?: return
        bodyDao.deletePhoto(id)
        withContext(ioDispatcher) {
            val file = File(photo.filePath)
            if (file.parentFile == photoDirectory) file.delete()
        }
    }

    private companion object {
        const val PHOTO_DIRECTORY = "photos"
        const val MAX_PHOTO_EDGE_PX = 2048
    }
}
