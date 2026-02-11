package com.inkstride.app.health

import android.content.Context

object StepsSyncer {

    suspend fun syncIfPermitted(context: Context): StepTotals? {
        val hc = HealthConnectManager(context)
        if (!hc.hasAllPermissions()) return null
        hc.onPermissionsGranted()
        return hc.getStepTotals()
    }
}
