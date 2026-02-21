package com.inkstride.app.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.repository.StoryRepository
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.ui.screens.JourneyScreen
import com.inkstride.app.ui.screens.StoryUnlockScreen
import kotlinx.coroutines.launch

private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

private enum class Screen {
    JOURNEY,
    INTRO
}

@Composable
fun AppRouter(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val healthConnectManager = remember { HealthConnectManager(context) }
    val storyRepository = remember { StoryRepository(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(Screen.JOURNEY) }
    var introSegmentId by remember { mutableStateOf<Int?>(null) }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun requestBgPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(context, BACKGROUND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) bgLauncher.launch(BACKGROUND_PERMISSION)
    }

    fun refreshRoute() {
        scope.launch {
            val intro = storyRepository.getIntroSegmentIfUnreadUnlocked()
            introSegmentId = intro?.id
            screen = if (intro != null) Screen.INTRO else Screen.JOURNEY
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        healthConnectManager.requestPermissionsActivityContract()
    ) {
        scope.launch {
            hasPermission = healthConnectManager.hasAllPermissions()
            if (hasPermission) {
                healthConnectManager.onPermissionsGranted()
                requestBgPermissionIfNeeded()
                refreshRoute()
            }
        }
    }

    LaunchedEffect(Unit) {
        DatabaseProvider.ensureDefaults(context)
        hasPermission = healthConnectManager.hasAllPermissions()
        if (hasPermission) {
            healthConnectManager.onPermissionsGranted()
            requestBgPermissionIfNeeded()
        }
        refreshRoute()
    }

    if (hasPermission && screen == Screen.INTRO && introSegmentId != null) {
        StoryUnlockScreen(
            modifier = Modifier.padding(innerPadding),
            storySegmentId = introSegmentId!!,
            onContinue = {
                scope.launch {
                    storyRepository.markAsRead(introSegmentId!!)
                    refreshRoute()
                }
            }
        )
        return
    }

    JourneyScreen(
        modifier = Modifier.padding(innerPadding),
        forcePermissionsPrompt = !hasPermission,
        onRequestPermissions = {
            permLauncher.launch(healthConnectManager.requiredPermissions())
        },
        onPotentialIntroUnlocked = {
            refreshRoute()
        }
    )
}