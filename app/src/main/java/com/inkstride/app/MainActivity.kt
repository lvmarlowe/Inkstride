package com.inkstride.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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

@Composable
fun JourneyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var totalSteps by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var backgroundReadEnabled by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        )
    }

    val loadSteps: suspend () -> Unit = {
        isLoading = true
        val journeyStartDate = getJourneyStartDate(context)
        totalSteps = healthConnectManager.getTotalSteps(journeyStartDate)
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
                saveJourneyStartDate(context)
                loadSteps()
                setupBackgroundSync()
            }
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = healthConnectManager.hasAllPermissions()
        if (hasPermission) {
            loadSteps()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = "$totalSteps",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "steps",
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

                    if (backgroundReadEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✓ Background sync enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun saveJourneyStartDate(context: Context) {
    val prefs = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)
    if (!prefs.contains("journey_start_date")) {
        prefs.edit().putString("journey_start_date", LocalDate.now().toString()).apply()
    }
}

private fun getJourneyStartDate(context: Context): LocalDate {
    val prefs = context.getSharedPreferences("inkstride_prefs", Context.MODE_PRIVATE)
    val dateString = prefs.getString("journey_start_date", LocalDate.now().toString())
    return LocalDate.parse(dateString)
}