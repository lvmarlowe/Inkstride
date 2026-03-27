package com.inkstride.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * rememberViewModel: Creates and remembers an activity-scoped view model using a lightweight factory lambda.
 * Reduces repeated ViewModelProvider.Factory boilerplate across screens and routers.
 */
@Composable
inline fun <reified VM : ViewModel> ComponentActivity.rememberViewModel(
    vararg keys: Any?,
    crossinline factory: () -> VM
): VM {
    return remember(*keys) {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return factory() as T
                }
            }
        )[VM::class.java]
    }
}