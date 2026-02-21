package com.inkstride.app.health

import android.content.Context

object StepsSyncer {

    suspend fun syncIfPermitted(context: Context): StepTotals? {
        val healthConnectManager = HealthConnectManager(context)
        if (!healthConnectManager.hasAllPermissions()) return null
        healthConnectManager.onPermissionsGranted()
        return healthConnectManager.getStepTotals()
    }
}