package com.inkstride.app.services

import com.inkstride.app.data.DistanceUnit
import kotlin.math.roundToInt

/**
 * DistanceUnitLabelFormatter: Formats distance unit labels for display based on value and unit type.
 * Applies singular and plural forms so UI text stays grammatically correct.
 */
class DistanceUnitLabelFormatter {

    // unitLabel: Returns the singular or plural unit label based on the rounded display value.
    fun unitLabel(distance: Double, distanceUnit: DistanceUnit): String {
        val isSingular = roundsToDisplayedSingleUnit(distance)
        return when (distanceUnit) {
            DistanceUnit.MILE -> if (isSingular) "mile" else "miles"
            DistanceUnit.KILOMETER -> if (isSingular) "kilometer" else "kilometers"
        }
    }

    // roundsToDisplayedSingleUnit: Returns true when the value rounds to exactly 1.0 at two decimal places.
    private fun roundsToDisplayedSingleUnit(distance: Double): Boolean {
        val roundedDistance = (distance * 100).roundToInt() / 100.0
        return roundedDistance == 1.0
    }
}