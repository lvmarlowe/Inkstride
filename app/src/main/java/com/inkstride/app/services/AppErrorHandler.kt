package com.inkstride.app.services

import android.util.Log

class AppErrorHandler {

    sealed class Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>()
        data class Failure(val shouldRetry: Boolean) : Outcome<Nothing>()
    }

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