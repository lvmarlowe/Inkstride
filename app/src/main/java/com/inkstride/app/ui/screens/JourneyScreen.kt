package com.inkstride.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.MainActivity
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.services.DistanceUnitLabelFormatter
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.ui.rememberViewModel
import com.inkstride.app.ui.viewmodels.JourneyEffect
import com.inkstride.app.ui.viewmodels.JourneyViewModel
import java.util.Locale

/**
 * JourneyScreen: Displays daily and total distance stats with pull-to-refresh and periodic sync.
 * Triggers story unlock callbacks when new segments or the intro become available after a sync.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JourneyScreen(
    modifier: Modifier = Modifier,
    onPermissionsRevoked: () -> Unit,
    onPotentialIntroUnlocked: () -> Unit,
    onNewStoryUnlocksFound: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val healthConnectManager = remember { HealthConnectManager(context) }
    val distanceUnitLabelFormatter = remember { DistanceUnitLabelFormatter() }

    val activity = context as MainActivity
    val journeyViewModel = activity.rememberViewModel(healthConnectManager) {
        JourneyViewModel(
            appContext = context.applicationContext,
            healthConnectManager = healthConnectManager
        )
    }

    val uiState by journeyViewModel.uiState.collectAsState()

    val pullState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = {
            journeyViewModel.onManualRefresh()
        }
    )

    // Runs once on composition to check permissions, load persisted data, and trigger initial sync.
    LaunchedEffect(journeyViewModel) {
        journeyViewModel.onScreenOpened()
    }

    // Collects one-time effects emitted by the journey view model and forwards callbacks to router-level handlers.
    LaunchedEffect(journeyViewModel) {
        journeyViewModel.effects.collect { effect ->
            when (effect) {
                JourneyEffect.PermissionsRevoked -> onPermissionsRevoked()
                JourneyEffect.PotentialIntroUnlocked -> onPotentialIntroUnlocked()
                JourneyEffect.NewStoryUnlocksFound -> onNewStoryUnlocksFound()
            }
        }
    }

    // Runs a sync on resume and repeats every foreground interval while the screen is active.
    LaunchedEffect(uiState.hasPermission, lifecycleOwner, journeyViewModel) {
        if (!uiState.hasPermission) return@LaunchedEffect

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            journeyViewModel.runForegroundSyncLoop()
        }
    }

    if (uiState.loading) {
        NeutralLoadingScreen(modifier = modifier)
        return
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Journey",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Day ${uiState.dayNumber}".uppercase(Locale.US),
                        color = Color(0x8CFFFFFF),
                        fontSize = 15.sp,
                        letterSpacing = 0.1.em,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (uiState.journeyAreaName.isNotBlank()) {
                        Text(
                            text = uiState.journeyAreaName,
                            color = Color(0x8CFFFFFF),
                            fontSize = 15.sp,
                            letterSpacing = 0.1.em,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(1.dp)
                            .background(Color(0x0DFFFFFF))
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    StatBlock(
                        label = "TODAY'S DISTANCE",
                        value = formatDistance(uiState.todayDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(uiState.todayDistance, uiState.distanceUnit)
                            .uppercase(Locale.US)
                    )

                    DividerLine()

                    StatBlock(
                        label = "TOTAL DISTANCE",
                        value = formatDistance(uiState.totalDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(uiState.totalDistance, uiState.distanceUnit)
                            .uppercase(Locale.US)
                    )

                    DividerLine()

                    StatBlock(
                        label = "NEXT MILESTONE",
                        value = formatDistance(uiState.nextMilestoneDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(uiState.nextMilestoneDistance, uiState.distanceUnit)
                            .uppercase(Locale.US)
                    )
                }
            }

            // Renders a timed fade-in status message anchored to the top of the screen.
            AnimatedVisibility(
                visible = uiState.flash.visible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xCC111111),
                    contentColor = if (uiState.flash.ok) Color.White else Color(0xFFFF8080),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = uiState.flash.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.refreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color.White,
                contentColor = Color.Black
            )
        }
    }
}

/**
 * StatBlock: Renders a labeled distance stat with a large value and unit label below it.
 * Used for today's distance, total distance, and next milestone display.
 */
@Composable
private fun StatBlock(
    label: String,
    value: String,
    unit: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0x99FFFFFF),
            fontSize = 14.sp,
            letterSpacing = 0.1.em,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 60.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = (60 * 1.05).sp
        )
        Text(
            text = unit,
            color = Color(0x80FFFFFF),
            fontSize = 14.sp,
            letterSpacing = 0.08.em,
            modifier = Modifier.padding(top = 0.dp)
        )
    }
}

// DividerLine: Renders a faint full-width horizontal rule to separate stat blocks.
@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x0DFFFFFF))
    )
}

// formatDistance: Formats a distance value to two decimal places for consistent display output.
private fun formatDistance(distance: Double): String {
    return String.format(Locale.US, "%.2f", distance)
}