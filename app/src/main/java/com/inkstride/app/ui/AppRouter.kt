package com.inkstride.app.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.repository.StoryRepository
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.ui.screens.JourneyScreen
import com.inkstride.app.ui.screens.PermissionsScreen
import com.inkstride.app.ui.screens.StoryUnlockScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
private const val PERMISSION_RECHECK_COUNT = 3
private const val PERMISSION_RECHECK_DELAY_MS = 220L

private enum class Screen {
    PERMISSIONS,
    JOURNEY,
    INTRO
}

@Composable
fun AppRouter(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val healthConnectManager = remember { HealthConnectManager(context) }
    val storyRepository = remember { StoryRepository(context) }

    var screen by remember { mutableStateOf<Screen?>(null) }
    var introSegmentId by remember { mutableStateOf<Int?>(null) }

    fun hasBackgroundPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, BACKGROUND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    var onBackgroundPermissionResult: (() -> Unit)? = null

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onBackgroundPermissionResult?.invoke()
    }

    fun refreshRoute(
        afterPermissionRequest: Boolean = false,
        afterBackgroundPermissionRequest: Boolean = false
    ) {
        screen = null
        scope.launch {
            var hasPermission = healthConnectManager.hasAllPermissions()
            if (afterPermissionRequest && !hasPermission) {
                repeat(PERMISSION_RECHECK_COUNT) {
                    delay(PERMISSION_RECHECK_DELAY_MS)
                    hasPermission = healthConnectManager.hasAllPermissions()
                    if (hasPermission) return@repeat
                }
            }

            if (!hasPermission) {
                introSegmentId = null
                screen = Screen.PERMISSIONS
                return@launch
            }

            healthConnectManager.onPermissionsGranted()
            val hasBackgroundPermission = hasBackgroundPermission()
            if (!hasBackgroundPermission && !afterBackgroundPermissionRequest) {
                bgLauncher.launch(BACKGROUND_PERMISSION)
                return@launch
            }

            val intro = storyRepository.getIntroSegmentIfUnreadUnlocked()
            introSegmentId = intro?.id
            screen = if (intro != null) Screen.INTRO else Screen.JOURNEY
        }
    }


    onBackgroundPermissionResult = {
        refreshRoute(afterBackgroundPermissionRequest = true)
    }

    val permLauncher = rememberLauncherForActivityResult(
        healthConnectManager.requestPermissionsActivityContract()
    ) {
        refreshRoute(afterPermissionRequest = true)
    }

    LaunchedEffect(Unit) {
        DatabaseProvider.ensureDefaults(context)
        refreshRoute()
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            refreshRoute()
        }
    }

    when (screen) {
        null -> NeutralLoadingScreen(
            modifier = Modifier.padding(innerPadding)
        )

        Screen.PERMISSIONS -> PermissionsScreen(
            modifier = Modifier.padding(innerPadding),
            onGrantPermissions = {
                permLauncher.launch(healthConnectManager.requiredPermissions())
            }
        )

        Screen.INTRO -> {
            val segmentId = introSegmentId
            if (segmentId != null) {
                StoryUnlockScreen(
                    modifier = Modifier.padding(innerPadding),
                    storySegmentId = segmentId,
                    onContinue = {
                        scope.launch {
                            storyRepository.markAsRead(segmentId)
                            refreshRoute()
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) { refreshRoute() }
            }
        }

        Screen.JOURNEY -> JourneyScreen(
            modifier = Modifier.padding(innerPadding),
            onPermissionsRevoked = {
                refreshRoute()
            },
            onPotentialIntroUnlocked = {
                refreshRoute()
            }
        )
    }
}
