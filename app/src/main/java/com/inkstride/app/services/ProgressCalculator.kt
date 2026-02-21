package com.inkstride.app.services

import kotlin.math.roundToInt

class ProgressCalculator {

    private val stepsPerMile = 2000.0

    fun stepsToDistance(steps: Long): Double {
        return steps / stepsPerMile
    }

    fun distanceToSteps(distance: Double): Long {
        return (distance * stepsPerMile).toLong()
    }

    fun getRemainingDistance(currentDistance: Double, nextMilestoneDistance: Double): Double {
        return (nextMilestoneDistance - currentDistance).coerceAtLeast(0.0)
    }

    fun roundDistance(distance: Double): Double {
        return (distance * 100).roundToInt() / 100.0
    }
}