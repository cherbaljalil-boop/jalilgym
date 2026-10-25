package com.chdjalil.jalilgym.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.chdjalil.jalilgym.core.domain.repository.HealthAvailability
import com.chdjalil.jalilgym.core.domain.repository.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.reflect.KClass

/** A weight another app wrote to Health Connect. */
data class ExternalWeight(val id: String, val time: Instant, val zoneOffset: ZoneOffset?, val kg: Double)

/** The few Health Connect calls Jalilgym makes, behind an interface so sync logic is testable. */
interface HealthConnectGateway {
    fun availability(): HealthAvailability

    suspend fun grantedPermissions(): Set<String>

    /** Inserts records; ones with a known client record ID and a higher version replace the old copy. */
    suspend fun upsert(records: List<Record>)

    suspend fun deleteByClientIds(type: KClass<out Record>, clientRecordIds: List<String>)

    suspend fun readWeightsFromOtherApps(since: Instant): List<ExternalWeight>

    suspend fun stepsBetween(start: Instant, end: Instant): Long?
}

/** Everything the integration asks for: write what we track, read weight and steps. */
val HEALTH_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    HealthPermission.getWritePermission(WeightRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getWritePermission(NutritionRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
)

internal class ClientHealthConnectGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthConnectGateway {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability(): HealthAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.NEEDS_INSTALL_OR_UPDATE
        else -> HealthAvailability.NOT_SUPPORTED
    }

    override suspend fun grantedPermissions(): Set<String> = client.permissionController.getGrantedPermissions()

    override suspend fun upsert(records: List<Record>) {
        // Health Connect limits batch sizes; stay well under it.
        records.chunked(BATCH).forEach { client.insertRecords(it) }
    }

    override suspend fun deleteByClientIds(type: KClass<out Record>, clientRecordIds: List<String>) {
        clientRecordIds.chunked(BATCH).forEach { client.deleteRecords(type, recordIdsList = emptyList(), clientRecordIdsList = it) }
    }

    override suspend fun readWeightsFromOtherApps(since: Instant): List<ExternalWeight> =
        client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.after(since)))
            .records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .map { ExternalWeight(it.metadata.id, it.time, it.zoneOffset, it.weight.inKilograms) }

    override suspend fun stepsBetween(start: Instant, end: Instant): Long? =
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, end)))[StepsRecord.COUNT_TOTAL]

    private companion object {
        const val BATCH = 500
    }
}

/** Health Connect's permission screen: launch with [HealthRepository.requiredPermissions], get back what was granted. */
fun healthPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
    androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

/** Play Store page for installing or updating Health Connect. */
const val HEALTH_CONNECT_STORE_URI = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
