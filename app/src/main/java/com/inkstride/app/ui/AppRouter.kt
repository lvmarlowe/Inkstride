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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.MainActivity
import com.inkstride.app.data.database.DatabaseProvider
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

// Defines the permission string for reading Health Connect data in the background.
private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

/**
 * AppRouter: Composes the active screen based on router state and handles permission launchers.
 * Initializes the database, wires permission results to the view model, and re-evaluates routes on resume.
 */
@Composable
fun AppRouter(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = context.applicationContext

    // Provides a UI-owned manager for permission contracts so the router view model builds its own app-context dependencies.
    val healthConnectManager = remember(appContext) { HealthConnectManager(appContext) }

    val activity = context as MainActivity
    val appRouterViewModel = activity.rememberViewModel(appContext) {
        AppRouterViewModel(appContext = appContext)
    }

    // Retrieves the single story repository instance owned by the router view model.
    val storyRepository = appRouterViewModel.getStoryRepository()

    val uiState by appRouterViewModel.uiState.collectAsState()

    // hasBackgroundPermission: Checks whether the background Health Connect permission is currently granted.
    fun hasBackgroundPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, BACKGROUND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Handles the background permission result and refreshes the route based on the outcome.
    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        appRouterViewModel.onBackgroundPermissionResult(
            hasBackgroundPermission = hasBackgroundPermission()
        )
    }

    // Handles the Health Connect permission result and refreshes the route based on the outcome.
    val permLauncher = rememberLauncherForActivityResult(
        healthConnectManager.requestPermissionsActivityContract()
    ) {
        appRouterViewModel.onPermissionsResult(
            hasBackgroundPermission = hasBackgroundPermission()
        )
    }

    // Collects one-time effects from the view model and launches the appropriate permission dialog.
    LaunchedEffect(appRouterViewModel) {
        appRouterViewModel.effects.collect { effect ->
            when (effect) {
                AppRouterEffect.RequestBackgroundPermission -> {
                    bgLauncher.launch(BACKGROUND_PERMISSION)
                }
            }
        }
    }

    // Seeds the database and performs the initial route evaluation on first composition.
    LaunchedEffect(Unit) {
        DatabaseProvider.ensureDefaults(context)
        appRouterViewModel.refreshRoute(hasBackgroundPermission = hasBackgroundPermission())
    }

    // Re-evaluates the route each time the app returns to the started state to catch permission changes.
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
            val introSegmentId = uiState.introSegmentId
            if (introSegmentId != null) {
                StoryUnlockScreen(
                    modifier = contentModifier,
                    storySegmentIds = listOf(introSegmentId),
                    onSegmentNavigatedAway = { },
                    onContinue = { appRouterViewModel.onIntroContinue() },
                    showForwardArrow = false,
                    title = "New Journey, New Story",
                    subtitle = "",
                    storyRepository = storyRepository
                )
            } else {
                // Re-checks route if intro state is unexpectedly empty.
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
                    onSegmentNavigatedAway = { segmentId ->
                        appRouterViewModel.onStoryUnlockSegmentNavigatedAway(segmentId)
                    },
                    showForwardArrow = true,
                    title = "A New Memory Made",
                    onContinue = { currentSegmentId ->
                        appRouterViewModel.onStoryUnlockContinueFromPager(currentSegmentId)
                    },
                    storyRepository = storyRepository
                )
            } else {
                // Clears the unlock session when segment ids are unexpectedly empty.
                LaunchedEffect(Unit) {
                    appRouterViewModel.onStoryUnlockContinueFromEmptySession()
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
                        },
                        onNewStoryUnlocksFound = {
                            appRouterViewModel.onPotentialNewUnlocks()
                        }
                    )

                    AppRouteScreen.STORYBOOK -> StorybookScreen(
                        storyRepository = storyRepository,
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
                    storyBadgeColor = uiState.storyBadgeColor,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}