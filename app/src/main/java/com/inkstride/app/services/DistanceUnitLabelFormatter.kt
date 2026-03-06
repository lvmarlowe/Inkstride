package com.inkstride.app.services

import com.inkstride.app.data.DistanceUnit
import kotlin.math.roundToInt

class DistanceUnitLabelFormatter {

    fun unitLabel(distance: Double, distanceUnit: DistanceUnit): String {
        val isSingular = roundsToDisplayedSingleUnit(distance)
        return when (distanceUnit) {
            DistanceUnit.MILE -> if (isSingular) "mile" else "miles"
            DistanceUnit.KILOMETER -> if (isSingular) "kilometer" else "kilometers"
        }
    }

    private fun roundsToDisplayedSingleUnit(distance: Double): Boolean {
        val roundedDistance = (distance * 100).roundToInt() / 100.0
        return roundedDistance == 1.0
    }
}