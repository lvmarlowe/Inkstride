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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.data.db.DatabaseProvider
import com.inkstride.app.data.db.entities.DistanceUnit
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.health.StepSyncCoordinator
import com.inkstride.app.health.StepSyncResult
import com.inkstride.app.health.StepSyncTrigger
import com.inkstride.app.health.StepsSyncScheduler
import com.inkstride.app.services.ProgressCalculator
import com.inkstride.app.services.DistanceUnitLabelFormatter
import com.inkstride.app.ui.components.NeutralLoadingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val healthConnectManager = remember { HealthConnectManager(context) }
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

    suspend fun sync(showFeedback: Boolean, trigger: StepSyncTrigger) {
        val result = StepSyncCoordinator.syncNow(context, trigger)
        when (result) {
            is StepSyncResult.Success -> {
                hasPermission = true
                dayNumber = result.snapshot.dayNumber
                todayDistance = result.snapshot.todayDistance
                totalDistance = result.snapshot.totalDistance
                nextMilestoneDistance = result.snapshot.nextMilestoneDistance
                distanceUnit = result.snapshot.distanceUnit

                if (result.snapshot.introUnlocked) {
                    onPotentialIntroUnlocked()
                }

                if (showFeedback) {
                    flash(true, "Synced")
                }
            }

            StepSyncResult.NoPermission -> {
                hasPermission = false
                onPermissionsRevoked()
                if (showFeedback) {
                    flash(false, "Permission required")
                }
            }

            StepSyncResult.SkippedAlreadyRunning -> {
                if (showFeedback) {
                    flash(true, "Sync already running")
                }
            }

            StepSyncResult.QueuedForRerun -> {
                if (showFeedback) {
                    flash(true, "Refresh queued")
                }
            }

            is StepSyncResult.Failure -> {
                if (showFeedback) {
                    flash(false, "Sync failed")
                }
            }
        }
    }

    suspend fun syncOnOpenWithRetry() {
        var attempt = 0
        while (attempt < 3) {
            val result = StepSyncCoordinator.syncNow(context, StepSyncTrigger.AUTOMATIC)
            when (result) {
                is StepSyncResult.Success -> {
                    hasPermission = true
                    dayNumber = result.snapshot.dayNumber
                    todayDistance = result.snapshot.todayDistance
                    totalDistance = result.snapshot.totalDistance
                    nextMilestoneDistance = result.snapshot.nextMilestoneDistance
                    distanceUnit = result.snapshot.distanceUnit

                    if (result.snapshot.introUnlocked) {
                        onPotentialIntroUnlocked()
                    }
                    return
                }

                StepSyncResult.NoPermission -> {
                    hasPermission = false
                    onPermissionsRevoked()
                    return
                }

                StepSyncResult.SkippedAlreadyRunning,
                StepSyncResult.QueuedForRerun,
                is StepSyncResult.Failure -> {
                    attempt += 1
                    if (attempt < 3) {
                        delay(900)
                    }
                }
            }
        }
    }

    suspend fun loadPersistedSnapshot() {
        val database = DatabaseProvider.getDatabase(context)
        val progressState = database.progressStateDao().get()
        val settings = database.settingsDao().get()

        distanceUnit = DistanceUnit.fromStorageValue(settings?.distanceUnit)

        if (progressState == null) {
            return
        }

        dayNumber = progressState.dayNumber
        totalDistance = progressState.totalDistance

        val todayKey = LocalDate.now().toString()
        todayDistance = database.dailyStatsDao().getByDate(todayKey)?.distanceToday ?: 0.0

        val nextMilestone = database.milestoneDao().getNextUnreached(totalDistance)
        val progressCalculator = ProgressCalculator()
        nextMilestoneDistance = progressCalculator.roundDistance(
            progressCalculator.getRemainingDistance(
                currentDistance = totalDistance,
                nextMilestoneDistance = nextMilestone?.distanceMarker ?: totalDistance
            )
        )
    }

    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    sync(showFeedback = true, trigger = StepSyncTrigger.MANUAL)
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

        loading = true
        try {
            loadPersistedSnapshot()
            syncOnOpenWithRetry()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sync(showFeedback = false, trigger = StepSyncTrigger.AUTOMATIC)
            while (true) {
                delay(TimeUnit.MINUTES.toMillis(FOREGROUND_SYNC_MINUTES))
                sync(showFeedback = false, trigger = StepSyncTrigger.AUTOMATIC)
            }
        }
    }

    DisposableEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    sync(showFeedback = false, trigger = StepSyncTrigger.AUTOMATIC)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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