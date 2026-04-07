package com.inkstride.app.services

import android.util.Log

/**
 * AppErrorHandler: Wraps suspend calls and returns a typed outcome instead of throwing.
 * Keeps error handling consistent across services so callers never deal with raw exceptions.
 */
class ErrorHandler {

    /**
     * Outcome: Represents the result of a wrapped suspend call.
     * Lets callers handle success and failure without catching exceptions directly.
     */
    sealed class Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>()
        data class Failure(val shouldRetry: Boolean) : Outcome<Nothing>()
    }

    // runSuspend: Runs the block and returns Success, or Failure with retry flag when an exception occurs.
    suspend fun <T> runSuspend(
        shouldRetry: Boolean = true,
        block: suspend () -> T
    ): Outcome<T> {
        return try {
            Outcome.Success(block())
        } catch (e: Exception) {
            Log.e("AppErrorHandler", "Unexpected error", e)
            Outcome.Failure(shouldRetry = shouldRetry)
        }
    }
}