package com.inkstride.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.health.ReadStepsWorker
import com.inkstride.app.ui.theme.InkstrideTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InkstrideTheme {
                JourneyScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JourneyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var cumulativeSteps by remember { mutableStateOf(0L) }
    var dailySteps by remember { mutableStateOf(0L) }
    var dayNumber by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var backgroundReadEnabled by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )
    }

    val loadSteps: suspend () -> Unit = {
        isLoading = true
        val journeyStartInstant = getJourneyStartInstant(context)
        cumulativeSteps = healthConnectManager.getTotalSteps(journeyStartInstant)
        dailySteps = healthConnectManager.getDailySteps(journeyStartInstant)
        dayNumber = calculateDayNumber(journeyStartInstant)
        isLoading = false
    }

    val setupBackgroundSync: () -> Unit = {
        val workRequest = PeriodicWorkRequestBuilder<ReadStepsWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueue(workRequest)
        backgroundReadEnabled = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        healthConnectManager.requestPermissionsActivityContract()
    ) {
        scope.launch {
            hasPermission = healthConnectManager.hasAllPermissions()
            if (hasPermission) {
                saveJourneyStartInstant(context)
                loadSteps()
                setupBackgroundSync()
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                if (hasPermission) {
                    loadSteps()
                }
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(Unit) {
        hasPermission = healthConnectManager.hasAllPermissions()
        if (hasPermission) {
            loadSteps()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            while (true) {
                delay(60000)
                loadSteps()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
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

                Spacer(modifier = Modifier.height(32.dp))

                if (!hasPermission) {
                    Text(
                        text = "Health Connect permissions required",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Grant permissions")
                    }
                } else {
                    if (isLoading && cumulativeSteps == 0L) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = "Day $dayNumber",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "$cumulativeSteps",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "total steps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "$dailySteps",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "steps today",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { scope.launch { loadSteps() } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color.White,
                contentColor = Color.Black
            )
        }
    }
}

private fun saveJourneyStartInstant(context: Context) {
    val prefs = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)
    if (!prefs.contains("journey_start_instant")) {
        prefs.edit().putString("journey_start_instant", Instant.now().toString()).apply()
    }
}

private fun getJourneyStartInstant(context: Context): Instant {
    val prefs = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)
    val instantString = prefs.getString("journey_start_instant", Instant.now().toString())
    return Instant.parse(instantString)
}

private fun calculateDayNumber(journeyStartInstant: Instant): Int {
    val zoneId = ZoneId.systemDefault()
    val startDate = journeyStartInstant.atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return ChronoUnit.DAYS.between(startDate, today).toInt() + 1
}