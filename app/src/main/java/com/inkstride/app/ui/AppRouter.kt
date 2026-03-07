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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.MainActivity
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.ui.components.BottomNavigationBar
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.ui.screens.JourneyScreen
import com.inkstride.app.ui.screens.PermissionsScreen
import com.inkstride.app.ui.screens.StoryUnlockScreen
import com.inkstride.app.ui.screens.StorybookScreen
import com.inkstride.app.ui.viewmodels.AppRouteScreen
import com.inkstride.app.ui.viewmodels.AppRouterEffect
import com.inkstride.app.ui.viewmodels.AppRouterViewModel

private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

@Composable
fun AppRouter(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val healthConnectManager = remember { HealthConnectManager(context) }
    val storyRepository = remember { StoryRepository(context) }

    val activity = context as MainActivity
    val appRouterViewModel = remember(activity, healthConnectManager, storyRepository) {
        ViewModelProvider(
            activity,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AppRouterViewModel(
                        healthConnectManager = healthConnectManager,
                        storyRepository = storyRepository
                    ) as T
                }
            }
        )[AppRouterViewModel::class.java]
    }

    val uiState by appRouterViewModel.uiState.collectAsState()

    fun hasBackgroundPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, BACKGROUND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        appRouterViewModel.onBackgroundPermissionResult(
            hasBackgroundPermission = hasBackgroundPermission()
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        healthConnectManager.requestPermissionsActivityContract()
    ) {
        appRouterViewModel.onPermissionsResult(
            hasBackgroundPermission = hasBackgroundPermission()
        )
    }

    LaunchedEffect(appRouterViewModel) {
        appRouterViewModel.effects.collect { effect ->
            when (effect) {
                AppRouterEffect.RequestBackgroundPermission -> {
                    bgLauncher.launch(BACKGROUND_PERMISSION)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        DatabaseProvider.ensureDefaults(context)
        appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
        }
    }

    val contentModifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)

    when (uiState.screen) {
        null -> NeutralLoadingScreen(
            modifier = contentModifier
        )

        AppRouteScreen.PERMISSIONS -> PermissionsScreen(
            modifier = contentModifier,
            onGrantPermissions = {
                permLauncher.launch(healthConnectManager.requiredPermissions())
            }
        )

        AppRouteScreen.INTRO -> {
            val segmentId = uiState.introSegmentId
            if (segmentId != null) {
                StoryUnlockScreen(
                    modifier = contentModifier,
                    storySegmentIds = listOf(segmentId),
                    onSegmentViewed = {},
                    showForwardArrow = false,
                    onContinue = {
                        appRouterViewModel.onIntroContinue()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
                }
            }
        }

        AppRouteScreen.STORY_UNLOCK -> {
            if (uiState.unreadUnlockSegmentIds.isNotEmpty()) {
                StoryUnlockScreen(
                    modifier = contentModifier,
                    storySegmentIds = uiState.unreadUnlockSegmentIds,
                    onSegmentViewed = { segmentId ->
                        appRouterViewModel.onStoryUnlockSegmentViewed(segmentId)
                    },
                    showForwardArrow = true,
                    title = "A New Memory Made",
                    onContinue = {
                        appRouterViewModel.onStoryUnlockContinue()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    appRouterViewModel.onStoryUnlockContinue()
                }
            }
        }

        AppRouteScreen.JOURNEY,
        AppRouteScreen.STORYBOOK -> {
            Box(modifier = contentModifier) {
                val isJourneySelected = uiState.screen == AppRouteScreen.JOURNEY

                when (uiState.screen) {
                    AppRouteScreen.JOURNEY -> JourneyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 88.dp),
                        onPermissionsRevoked = {
                            appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
                        },
                        onPotentialIntroUnlocked = {
                            appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
                        }
                    )

                    AppRouteScreen.STORYBOOK -> StorybookScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 88.dp)
                    )

                    else -> Unit
                }

                BottomNavigationBar(
                    onJourneyClick = { appRouterViewModel.onJourneySelected() },
                    onStorybookClick = { appRouterViewModel.openStoryDestinationFromNavigation() },
                    hasStorybookNotification = uiState.hasStoryNotification,
                    isJourneySelected = isJourneySelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}