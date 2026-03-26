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
import com.inkstride.app.services.DataValidator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * StepTotals: Stores cumulative and current-day step totals from a Health Connect sync.
 * Keeps sync output grouped in one value passed to repositories.
 */
data class StepTotals(
    val cumulativeSteps: Long,
    val todaySteps: Long
)

/**
 * HealthConnectManager: Reads step data from Health Connect and tracks journey timing.
 * Persists journey start so day numbering stays stable across app restarts.
 */
class HealthConnectManager(context: Context) {

    // Accesses Health Connect aggregate and permission APIs.
    private val client = HealthConnectClient.getOrCreate(context)

    // Stores journey start so timeline calculations stay consistent across sessions.
    private val preferences = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)

    // requestPermissionsActivityContract: Returns the permission request contract used by the UI layer.
    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    // requiredPermissions: Returns the set of permissions required for step reads.
    fun requiredPermissions(): Set<String> =
        setOf(HealthPermission.getReadPermission(StepsRecord::class))

    // hasAllPermissions: Returns true when all required permissions are granted.
    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(requiredPermissions())

    // onPermissionsGranted: Saves a journey start value once permissions are available.
    fun onPermissionsGranted() {
        ensureJourneyStartInstant()
    }

    /**
     * getStepTotals: Returns cumulative and current-day steps from Health Connect.
     * Uses local midnight as the day boundary except on day one, where journey start is used
     * so early progress is not lost.
     */
    suspend fun getStepTotals(): StepTotals {
        val journeyStart = ensureJourneyStartInstant()
        val now = Instant.now()
        val todayStart = todayStartInstant(journeyStart)

        val cumulative = readSteps(journeyStart, now)
        val today = readSteps(todayStart, now)

        return StepTotals(
            cumulativeSteps = cumulative,
            todaySteps = today
        )
    }

    // getJourneyStartInstant: Returns the stored journey start instant for day number calculations.
    fun getJourneyStartInstant(): Instant {
        return ensureJourneyStartInstant()
    }

    // readSteps: Returns step count from Health Connect for the provided time range.
    private suspend fun readSteps(start: Instant, end: Instant): Long {
        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = client.aggregate(request)
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    /**
     * ensureJourneyStartInstant: Returns the existing journey start when present.
     * Stores the current instant on first call so the start time is captured once and reused.
     */
    private fun ensureJourneyStartInstant(): Instant {
        val existing = preferences.getString("journey_start_instant", null)
        if (!existing.isNullOrBlank()) return Instant.parse(existing)

        val now = Instant.now()
        preferences.edit { putString("journey_start_instant", now.toString()) }
        return now
    }

    /**
     * todayStartInstant: Returns the effective start instant for the current day.
     * Uses journey start on day one so steps recorded before midnight are not excluded.
     */
    private fun todayStartInstant(journeyStart: Instant): Instant {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val journeyDay = journeyStart.atZone(zone).toLocalDate()
        return if (today == journeyDay) {
            journeyStart
        } else {
            today.atStartOfDay(zone).toInstant()
        }
    }

    companion object {
        /**
         * computeDayNumberFromJourneyStart: Returns a one-based day number from journey start.
         * Normalizes the value so it never drops below day one even if the clock or date is inconsistent.
         */
        fun computeDayNumberFromJourneyStart(journeyStart: Instant): Int {
            val zone = ZoneId.systemDefault()
            val startDate = journeyStart.atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            val daysBetween = ChronoUnit.DAYS.between(startDate, today)
            val dataValidator = DataValidator()
            return dataValidator.coerceDayNumber((daysBetween + 1).toInt())
        }
    }
}