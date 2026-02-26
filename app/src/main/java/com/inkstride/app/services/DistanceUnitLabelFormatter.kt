package com.inkstride.app.services

import com.inkstride.app.data.db.entities.DistanceUnit
import java.util.Locale

class DistanceUnitLabelFormatter {

    fun unitLabel(distance: Double, distanceUnit: DistanceUnit): String {
        val isSingular = formatDistance(distance) == "1.00"
        return when (distanceUnit) {
            DistanceUnit.MILE -> if (isSingular) "mile" else "miles"
            DistanceUnit.KILOMETER -> if (isSingular) "kilometer" else "kilometers"
        }
    }

    private fun formatDistance(distance: Double): String {
        return String.format(Locale.US, "%.2f", distance)
    }
}
