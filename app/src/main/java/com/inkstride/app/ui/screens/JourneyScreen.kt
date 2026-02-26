package com.inkstride.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.db.entities.DistanceUnit
import com.inkstride.app.data.repository.ProgressRepository
import com.inkstride.app.data.repository.StoryRepository
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.health.StepsSyncScheduler
import com.inkstride.app.health.StepsSyncer
import com.inkstride.app.services.AppErrorHandler
import com.inkstride.app.services.DistanceUnitLabelFormatter
import com.inkstride.app.services.MilestoneEngine
import com.inkstride.app.services.ProgressCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val FOREGROUND_SYNC_MINUTES = 5L

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JourneyScreen(
    modifier: Modifier = Modifier,
    onPermissionsRevoked: () -> Unit,
    onPotentialIntroUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val errorHandler = remember { AppErrorHandler() }
    val healthConnectManager = remember { HealthConnectManager(context) }

    val database = remember { DatabaseProvider.getDatabase(context) }

    val progressRepository = remember {
        ProgressRepository(
            context = context,
            progressStateDao = database.progressStateDao(),
            dailyStatsDao = database.dailyStatsDao()
        )
    }

    val storyRepository = remember { StoryRepository(context) }
    val milestoneEngine = remember { MilestoneEngine(context) }
    val progressCalculator = remember { ProgressCalculator() }
    val distanceUnitLabelFormatter = remember { DistanceUnitLabelFormatter() }

    var dayNumber by remember { mutableIntStateOf(1) }
    var hasPermission by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var totalDistance by remember { mutableDoubleStateOf(0.0) }
    var todayDistance by remember { mutableDoubleStateOf(0.0) }
    var nextMilestoneDistance by remember { mutableDoubleStateOf(0.0) }
    var distanceUnit by remember { mutableStateOf(DistanceUnit.MILE) }

    var showMsg by remember { mutableStateOf(false) }
    var msgOk by remember { mutableStateOf(true) }
    var msgText by remember { mutableStateOf("") }

    fun flash(ok: Boolean, text: String) {
        msgOk = ok
        msgText = text
        showMsg = true
        scope.launch {
            delay(1800)
            showMsg = false
        }
    }

    suspend fun sync(showFeedback: Boolean) {
        if (loading) return
        loading = true

        val outcome = errorHandler.runSuspend(shouldRetry = false) {
            val totals = StepsSyncer.syncIfPermitted(context)
            hasPermission = totals != null
            if (totals == null) {
                onPermissionsRevoked()
                return@runSuspend
            }

            todayDistance = progressCalculator.roundDistance(progressCalculator.stepsToDistance(totals.todaySteps))

            val journeyStart = healthConnectManager.getJourneyStartInstant()
            dayNumber = HealthConnectManager.computeDayNumberFromJourneyStart(journeyStart)

            totalDistance = progressRepository.persistSnapshotFromHealthConnect(
                stepTotals = totals,
                dayNumber = dayNumber
            )

            val nextMilestone = database.milestoneDao().getNextUnreached(totalDistance)
            nextMilestoneDistance = progressCalculator.roundDistance(
                progressCalculator.getRemainingDistance(
                    currentDistance = totalDistance,
                    nextMilestoneDistance = nextMilestone?.distanceMarker ?: totalDistance
                )
            )

            val rawDistanceUnit = database.settingsDao().get()?.distanceUnit
            distanceUnit = DistanceUnit.fromStorageValue(rawDistanceUnit)

            milestoneEngine.checkAndUnlockForDistance(totalDistance)

            val intro = storyRepository.getIntroSegmentIfUnreadUnlocked()
            if (intro != null) {
                onPotentialIntroUnlocked()
            }

            if (showFeedback) flash(true, "Synced")
        }

        if (outcome is AppErrorHandler.Outcome.Failure) {
            if (showFeedback) flash(false, "Sync failed")
        }

        loading = false
    }

    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    sync(showFeedback = true)
                } finally {
                    refreshing = false
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        DatabaseProvider.ensureDefaults(context)

        hasPermission = healthConnectManager.hasAllPermissions()
        if (hasPermission) {
            healthConnectManager.onPermissionsGranted()
            StepsSyncScheduler.schedule(context)
        }

        sync(showFeedback = false)
    }

    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sync(showFeedback = false)
            while (true) {
                delay(TimeUnit.MINUTES.toMillis(FOREGROUND_SYNC_MINUTES))
                sync(showFeedback = false)
            }
        }
    }

    if (loading || refreshing) {
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Journey",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (hasPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Day $dayNumber",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    "Today's distance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = formatDistance(todayDistance),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    distanceUnitLabelFormatter.unitLabel(todayDistance, distanceUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    "Total distance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = formatDistance(totalDistance),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    distanceUnitLabelFormatter.unitLabel(totalDistance, distanceUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    "Next milestone in",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = formatDistance(nextMilestoneDistance),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    distanceUnitLabelFormatter.unitLabel(nextMilestoneDistance, distanceUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(240.dp))
            }

            AnimatedVisibility(
                visible = showMsg,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xCC111111),
                    contentColor = if (msgOk) Color.White else Color(0xFFFF8080),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = msgText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color.White,
                contentColor = Color.Black
            )
        }
    }
}

private fun formatDistance(distance: Double): String {
    return String.format(Locale.US, "%.2f", distance)
}
