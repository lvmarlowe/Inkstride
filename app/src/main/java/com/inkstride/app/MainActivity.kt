package com.inkstride.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.health.StepsSyncScheduler
import com.inkstride.app.health.StepsSyncer
import com.inkstride.app.ui.theme.InkstrideTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max

private const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
private const val FOREGROUND_SYNC_MINUTES = 5L

private const val PREFS_NAME = "inkstride_prefs"
private const val PERM_GRANTED_TS_KEY = "perm_granted_ts"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkstrideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) { JourneyScreen() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JourneyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val hc = remember { HealthConnectManager(context) }

    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun ensurePermissionTimestamp() {
        val existing = prefs.getLong(PERM_GRANTED_TS_KEY, 0L)
        if (existing == 0L) {
            prefs.edit().putLong(PERM_GRANTED_TS_KEY, System.currentTimeMillis()).apply()
        }
    }

    fun computeDayNumber(): Int {
        val ts = prefs.getLong(PERM_GRANTED_TS_KEY, 0L)
        if (ts == 0L) return 1
        val days = ((System.currentTimeMillis() - ts) / TimeUnit.DAYS.toMillis(1)) + 1
        return max(1, days.toInt())
    }

    var dayNumber by remember { mutableStateOf(1) }

    fun refreshDayNumber() {
        dayNumber = computeDayNumber()
    }

    /*
    // DEBUG helper (temporary): force the permission-grant timestamp back a few days.
    fun forcePermissionTimestampDaysAgo(daysAgo: Int) {
        val ts = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysAgo.toLong())
        prefs.edit().putLong(PERM_GRANTED_TS_KEY, ts).apply()
        dayNumber = computeDayNumber()
    }
    */

    var hasPerm by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var total by remember { mutableLongStateOf(0L) }
    var today by remember { mutableLongStateOf(0L) }

    var showMsg by remember { mutableStateOf(false) }
    var msgOk by remember { mutableStateOf(true) }
    var msgText by remember { mutableStateOf("") }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun requestBgPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(context, BACKGROUND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) bgLauncher.launch(BACKGROUND_PERMISSION)
    }

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
        try {
            val totals = StepsSyncer.syncIfPermitted(context)
            hasPerm = totals != null
            if (totals != null) {
                total = totals.cumulativeSteps
                today = totals.todaySteps
                if (showFeedback) flash(true, "Synced")
            }
        } catch (_: Exception) {
            if (showFeedback) flash(false, "Sync failed")
        } finally {
            loading = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        hc.requestPermissionsActivityContract()
    ) {
        scope.launch {
            hasPerm = hc.hasAllPermissions()
            if (hasPerm) {
                hc.onPermissionsGranted()
                ensurePermissionTimestamp()
                refreshDayNumber()

                requestBgPermissionIfNeeded()
                StepsSyncScheduler.schedule(context)
                sync(showFeedback = true)
            }
        }
    }

    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    sync(showFeedback = true)
                    refreshDayNumber()
                } finally {
                    refreshing = false
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        hasPerm = hc.hasAllPermissions()
        if (hasPerm) {
            hc.onPermissionsGranted()
            ensurePermissionTimestamp()
            refreshDayNumber()

            requestBgPermissionIfNeeded()
            StepsSyncScheduler.schedule(context)
        }
        sync(showFeedback = false)
    }

    LaunchedEffect(hasPerm, lifecycleOwner) {
        if (!hasPerm) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sync(showFeedback = false)
            while (true) {
                delay(TimeUnit.MINUTES.toMillis(FOREGROUND_SYNC_MINUTES))
                sync(showFeedback = false)
                refreshDayNumber()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
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

                if (hasPerm) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Day $dayNumber",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    /*
                    // DEBUG button (temporary): set start date back 4 days.
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { forcePermissionTimestampDaysAgo(4) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text("DEBUG: Set Start Date -4 Days")
                    }
                    */
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (!hasPerm) {
                    Text(text = "Health Connect permissions required", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permLauncher.launch(hc.requiredPermissions()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) { Text("Grant permissions") }
                } else {
                    if (loading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = "$total",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("total steps", style = MaterialTheme.typography.bodyLarge, color = Color.White)

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "$today",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("steps today", style = MaterialTheme.typography.bodyLarge, color = Color.White)

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = { scope.launch { sync(showFeedback = true) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) { Text("Refresh") }
                    }
                }

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