package com.inkstride.app.services

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
        } catch (_: Exception) {
            Outcome.Failure(shouldRetry = shouldRetry)
        }
    }
}