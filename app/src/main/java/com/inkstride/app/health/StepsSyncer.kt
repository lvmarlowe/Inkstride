package com.inkstride.app.health

import android.content.Context

/**
 * StepsSyncer: Checks Health Connect permissions and reads step totals when permitted.
 * Keeps permission checks and step reads together so callers receive data or null in one call.
 */
object StepsSyncer {

    // syncIfPermitted: Returns step totals when permissions are granted, or null when they are not.
    suspend fun syncIfPermitted(context: Context): StepTotals? {
        val healthConnectManager = HealthConnectManager(context)
        if (!healthConnectManager.hasAllPermissions()) return null
        healthConnectManager.onPermissionsGranted()
        return healthConnectManager.getStepTotals()
    }
}