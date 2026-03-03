package com.inkstride.app.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.repository.StoryRepository
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.ui.components.BottomNavigationBar
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.ui.screens.JourneyScreen
import com.inkstride.app.ui.screens.PermissionsScreen
import com.inkstride.app.ui.screens.StoryUnlockScreen
import com.inkstride.app.ui.screens.StorybookScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
private const val PERMISSION_RECHECK_COUNT = 3
private const val PERMISSION_RECHECK_DELAY_MS = 220L

private enum class Screen {
    PERMISSIONS,
    JOURNEY,
    STORYBOOK,
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
        val previousScreen = screen
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
            screen = if (intro != null) Screen.INTRO else if (previousScreen == Screen.STORYBOOK) Screen.STORYBOOK else Screen.JOURNEY
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

    val contentModifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)

    when (screen) {
        null -> NeutralLoadingScreen(
            modifier = contentModifier
        )

        Screen.PERMISSIONS -> PermissionsScreen(
            modifier = contentModifier,
            onGrantPermissions = {
                permLauncher.launch(healthConnectManager.requiredPermissions())
            }
        )

        Screen.INTRO -> {
            val segmentId = introSegmentId
            if (segmentId != null) {
                StoryUnlockScreen(
                    modifier = contentModifier,
                    storySegmentId = segmentId,
                    onContinue = {
                        scope.launch {
                            storyRepository.markAsRead(segmentId)
                            screen = Screen.JOURNEY
                            refreshRoute()
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) { refreshRoute() }
            }
        }

        Screen.JOURNEY,
        Screen.STORYBOOK -> {
            Box(modifier = contentModifier) {
                when (screen) {
                    Screen.JOURNEY -> JourneyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp),
                        onPermissionsRevoked = {
                            refreshRoute()
                        },
                        onPotentialIntroUnlocked = {
                            refreshRoute()
                        }
                    )

                    Screen.STORYBOOK -> StorybookScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp)
                    )

                    else -> Unit
                }

                BottomNavigationBar(
                    onJourneyClick = { screen = Screen.JOURNEY },
                    onStorybookClick = { screen = Screen.STORYBOOK },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}