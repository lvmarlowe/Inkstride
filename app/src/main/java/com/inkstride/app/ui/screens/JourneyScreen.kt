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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.DistanceUnit
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
    onPotentialIntroUnlocked: () -> Unit,
    onNewStoryUnlocksFound: () -> Unit
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
    var journeyAreaName by remember { mutableStateOf("") }

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

    fun applySnapshot(result: StepSyncResult.Success) {
        hasPermission = true
        dayNumber = result.snapshot.dayNumber
        todayDistance = result.snapshot.todayDistance
        totalDistance = result.snapshot.totalDistance
        nextMilestoneDistance = result.snapshot.nextMilestoneDistance
        distanceUnit = result.snapshot.distanceUnit
        journeyAreaName = result.snapshot.currentAreaName

        if (result.snapshot.introUnlocked) {
            onPotentialIntroUnlocked()
        }
        if (result.snapshot.newUnlocksFound) {
            onNewStoryUnlocksFound()
        }
    }

    suspend fun sync(showFeedback: Boolean, trigger: StepSyncTrigger) {
        val result = StepSyncCoordinator.syncNow(context, trigger)
        when (result) {
            is StepSyncResult.Success -> {
                applySnapshot(result)

                if (showFeedback) {
                    flash(true, "Synced")
                }
            }

            StepSyncResult.NoPermission -> {
                hasPermission = false
                onPermissionsRevoked()
            }

            StepSyncResult.SkippedAlreadyRunning -> { }

            StepSyncResult.QueuedForRerun -> { }

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
                    applySnapshot(result)
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

        val milestoneDao = database.milestoneDao()
        val nextMilestone = milestoneDao.getNextUnreached(totalDistance)
        journeyAreaName = milestoneDao.getLatestPersistentUnlockedAreaName().orEmpty()
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

    if (loading) {
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
                        text = "Day $dayNumber".uppercase(Locale.US),
                        color = Color(0x8CFFFFFF),
                        fontSize = 15.sp,
                        letterSpacing = 0.1.em,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (journeyAreaName.isNotBlank()) {
                        Text(
                            text = journeyAreaName,
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
                        value = formatDistance(todayDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(todayDistance, distanceUnit)
                            .uppercase(Locale.US)
                    )

                    DividerLine()

                    StatBlock(
                        label = "TOTAL DISTANCE",
                        value = formatDistance(totalDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(totalDistance, distanceUnit)
                            .uppercase(Locale.US)
                    )

                    DividerLine()

                    StatBlock(
                        label = "NEXT MILESTONE IN",
                        value = formatDistance(nextMilestoneDistance),
                        unit = distanceUnitLabelFormatter
                            .unitLabel(nextMilestoneDistance, distanceUnit)
                            .uppercase(Locale.US)
                    )
                }
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
                    modifier = Modifier.padding(top = 16.dp)
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

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x0DFFFFFF))
    )
}

private fun formatDistance(distance: Double): String {
    return String.format(Locale.US, "%.2f", distance)
}