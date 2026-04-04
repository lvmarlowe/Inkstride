package com.inkstride.app.services

import kotlin.math.roundToInt

/**
 * ProgressCalculator: Handles step and distance math used by sync and progression flows.
 * Keeps conversion behavior small and reusable so results stay consistent across the app.
 */
class ProgressCalculator {

    // Uses one conversion factor so all step-to-distance math stays consistent.
    private val stepsPerMile = 2000.0

    // Reuses validation behavior across calculations to keep distance flooring consistent.
    private val dataValidator = DataValidator()

    // stepsToDistance: Returns distance in miles converted from a step count.
    fun stepsToDistance(steps: Long): Double {
        return steps / stepsPerMile
    }

    /**
     * getRemainingDistance: Returns remaining distance to the next milestone.
     * Floors at zero so UI and unlock logic never receive a negative distance value.
     */
    fun getRemainingDistance(currentDistance: Double, nextMilestoneDistance: Double): Double {
        return dataValidator.coerceDistance(nextMilestoneDistance - currentDistance)
    }

    // roundDistance: Returns distance rounded to two decimal places for consistent display output.
    fun roundDistance(distance: Double): Double {
        return (distance * 100).roundToInt() / 100.0
    }
}