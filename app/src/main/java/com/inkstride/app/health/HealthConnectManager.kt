package com.inkstride.app.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StepTotals(
    val cumulativeSteps: Long,
    val todaySteps: Long
)

class HealthConnectManager(context: Context) {

    private val client = HealthConnectClient.getOrCreate(context)
    private val prefs = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    fun requiredPermissions(): Set<String> =
        setOf(HealthPermission.getReadPermission(StepsRecord::class))

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(requiredPermissions())

    fun onPermissionsGranted() {
        ensureJourneyStartInstant()
    }

    suspend fun getStepTotals(): StepTotals {
        val journeyStart = ensureJourneyStartInstant()
        val now = Instant.now()
        val todayStart = todayStartInstant(journeyStart)

        val cumulative = readSteps(journeyStart, now)
        val today = readSteps(todayStart, now)

        return StepTotals(cumulativeSteps = cumulative, todaySteps = today)
    }

    private suspend fun readSteps(start: Instant, end: Instant): Long {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = client.aggregate(request)
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    private fun ensureJourneyStartInstant(): Instant {
        val existing = prefs.getString("journey_start_instant", null)
        if (!existing.isNullOrBlank()) return Instant.parse(existing)

        val now = Instant.now()
        prefs.edit { putString("journey_start_instant", now.toString()) }
        return now
    }

    private fun todayStartInstant(journeyStart: Instant): Instant {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val journeyDay = journeyStart.atZone(zone).toLocalDate()
        return if (today == journeyDay) journeyStart else today.atStartOfDay(zone).toInstant()
    }
}
