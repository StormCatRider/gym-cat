/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — data/HealthConnectManager.kt                 ║
║ The link with Gym & Ñam through Android's Health Connect.          ║
║ Gym Cat (your choices):                                            ║
║  • WRITES ActiveCaloriesBurnedRecord for each workout → Gym & Ñam ║
║    adds those calories to your day automatically.                 ║
║  • READS WeightRecord (body weight) and NutritionRecord (protein). ║
║  • DELETES only records Gym Cat wrote (by their saved id).        ║
║ Everything is OFF until you turn Health Connect on in Settings,    ║
║ and only these 3 permissions are requested.                        ║
║ Metadata: manualEntry() for workouts you log by hand;              ║
║ activelyRecorded() for GPS runs the phone tracked live.            ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    /** The exact permissions Gym Cat asks for — nothing more. */
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /** Used by the "allow" screen in Settings. */
    fun requestContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun granted(): Set<String> =
        if (!isAvailable()) emptySet() else runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())

    suspend fun hasAll(): Boolean = granted().containsAll(permissions)

    /**
     * Writes calories for one workout. Returns the record id (saved on the workout)
     * or "" if not written. clientRecordId = "gymcat-workout-<id>" so re-writing
     * the same workout replaces it instead of duplicating it.
     */
    suspend fun writeCalories(workoutId: Long, startMs: Long, endMs: Long, kcal: Double, liveGps: Boolean): String {
        if (!isAvailable() || kcal <= 0 || endMs <= startMs) return ""
        if (!granted().contains(HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class))) return ""
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMs); val end = Instant.ofEpochMilli(endMs)
        val clientId = "gymcat-workout-$workoutId"
        val metadata = if (liveGps)
            Metadata.activelyRecorded(clientRecordId = clientId, clientRecordVersion = 1L, device = Device(type = Device.TYPE_PHONE))
        else Metadata.manualEntry(clientRecordId = clientId, clientRecordVersion = 1L)
        val record = ActiveCaloriesBurnedRecord(
            startTime = start, startZoneOffset = zone.rules.getOffset(start),
            endTime = end, endZoneOffset = zone.rules.getOffset(end),
            energy = Energy.kilocalories(kcal),
            metadata = metadata,
        )
        return runCatching { client.insertRecords(listOf(record)).recordIdsList.firstOrNull() ?: "" }.getOrDefault("")
    }

    /** Deletes ONLY a record Gym Cat wrote (we pass the id we saved). */
    suspend fun deleteOwn(recordId: String) {
        if (recordId.isBlank() || !isAvailable()) return
        runCatching {
            client.deleteRecords(ActiveCaloriesBurnedRecord::class, recordIdsList = listOf(recordId), clientRecordIdsList = emptyList())
        }
    }

    /** Latest body weight in the last 60 days (written by Gym & Ñam or any app). lb + epochDay. */
    suspend fun latestWeight(): Pair<Double, Long>? {
        if (!granted().contains(HealthPermission.getReadPermission(WeightRecord::class))) return null
        val now = Instant.now()
        return runCatching {
            val r = client.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(now.minusSeconds(60L * 86400), now),
                    ascendingOrder = false, pageSize = 1)
            ).records.firstOrNull() ?: return null
            r.weight.inPounds to r.time.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        }.getOrNull()
    }

    /** Body weight history for the chart: (epochDay, lb), oldest first, last N days. */
    suspend fun weightHistory(days: Int = 180): List<Pair<Long, Double>> {
        if (!granted().contains(HealthPermission.getReadPermission(WeightRecord::class))) return emptyList()
        val now = Instant.now()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(now.minusSeconds(days * 86400L), now),
                    ascendingOrder = true, pageSize = 1000)
            ).records.map { it.time.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() to it.weight.inPounds }
        }.getOrDefault(emptyList())
    }

    /** Protein eaten today (grams), summed from NutritionRecord (Gym & Ñam writes these). */
    suspend fun proteinToday(): Double? {
        if (!granted().contains(HealthPermission.getReadPermission(NutritionRecord::class))) return null
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        return runCatching {
            client.aggregate(AggregateRequest(setOf(NutritionRecord.PROTEIN_TOTAL), TimeRangeFilter.between(start, Instant.now())))[
                NutritionRecord.PROTEIN_TOTAL]?.inGrams
        }.getOrNull()
    }

}
