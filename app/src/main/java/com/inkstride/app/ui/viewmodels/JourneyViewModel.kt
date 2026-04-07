package com.inkstride.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkstride.app.data.DistanceUnit
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.health.HealthConnectManager
import com.inkstride.app.health.StepSyncCoordinator
import com.inkstride.app.health.StepSyncResult
import com.inkstride.app.health.StepSyncTrigger
import com.inkstride.app.health.StepsSyncScheduler
import com.inkstride.app.services.ProgressCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// Interval in minutes between automatic foreground syncs while the screen is active.
private const val FOREGROUND_SYNC_MINUTES = 5L

// Number of open-sync attempts used to absorb brief startup races.
private const val OPEN_SYNC_MAX_ATTEMPTS = 3

// Delay in milliseconds between open-sync retry attempts.
private const val OPEN_SYNC_RETRY_DELAY_MS = 900L

// Duration in milliseconds that a status message remains visible.
private const val FLASH_DURATION_MS = 1800L

/**
 * JourneyFlashState: Represents the transient status message shown at the top of the journey screen.
 */
data class JourneyFlashState(
    val visible: Boolean = false,
    val ok: Boolean = true,
    val text: String = ""
)

/**
 * JourneyUiState: Holds all state required to render the journey screen.
 */
data class JourneyUiState(
    val dayNumber: Int = 1,
    val todayDistance: Double = 0.0,
    val totalDistance: Double = 0.0,
    val nextMilestoneDistance: Double = 0.0,
    val distanceUnit: DistanceUnit = DistanceUnit.MILE,
    val journeyAreaName: String = "",
    val hasPermission: Boolean = false,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val flash: JourneyFlashState = JourneyFlashState()
)

/**
 * JourneyEffect: Holds one-time effects emitted from JourneyViewModel to the screen layer.
 */
sealed interface JourneyEffect {
    data object PermissionsRevoked : JourneyEffect
    data object PotentialIntroUnlocked : JourneyEffect
    data object NewStoryUnlocksFound : JourneyEffect
}

/**
 * JourneyViewModel: Owns journey screen state, sync orchestration, refresh handling, and flash messaging.
 */
class JourneyViewModel(
    private val appContext: Context,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    companion object {
        private var cachedSnapshot: PersistedJourneySnapshot? = null

        suspend fun warmCache(appContext: Context) {
            if (cachedSnapshot != null) return
            cachedSnapshot = JourneySnapshotRepository(appContext).loadPersistedSnapshot()
        }
    }

    private val journeySnapshotRepository = JourneySnapshotRepository(appContext)

    private val _uiState = MutableStateFlow(
        cachedSnapshot?.toJourneyUiState() ?: JourneyUiState()
    )
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<JourneyEffect>()
    val effects: SharedFlow<JourneyEffect> = _effects.asSharedFlow()

    /**
     * onScreenOpened: Checks permission state, loads persisted progress, and starts open-sync retry flow.
     */
    fun onScreenOpened() {
        viewModelScope.launch {
            val hasPermission = healthConnectManager.hasAllPermissions()
            _uiState.update { it.copy(hasPermission = hasPermission) }

            if (hasPermission) {
                healthConnectManager.onPermissionsGranted()
                StepsSyncScheduler.schedule(appContext)
            }

            _uiState.update { it.copy(loading = true) }
            try {
                loadPersistedSnapshot()
                syncOnOpenWithRetry()
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    // onManualRefresh: Runs a user-triggered sync and shows explicit success/failure feedback.
    fun onManualRefresh() {
        if (_uiState.value.refreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            try {
                sync(showFeedback = true, trigger = StepSyncTrigger.MANUAL)
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

    /**
     * runForegroundSyncLoop: Triggers one immediate automatic sync and repeats while the caller is active.
     */
    suspend fun runForegroundSyncLoop() {
        if (!_uiState.value.hasPermission) return

        sync(showFeedback = false, trigger = StepSyncTrigger.AUTOMATIC)
        while (true) {
            delay(TimeUnit.MINUTES.toMillis(FOREGROUND_SYNC_MINUTES))
            sync(showFeedback = false, trigger = StepSyncTrigger.AUTOMATIC)
        }
    }

    // flash: Shows a timed status message and clears it after FLASH_DURATION_MS.
    private fun flash(ok: Boolean, text: String) {
        _uiState.update {
            it.copy(
                flash = JourneyFlashState(
                    visible = true,
                    ok = ok,
                    text = text
                )
            )
        }

        viewModelScope.launch {
            delay(FLASH_DURATION_MS)
            _uiState.update {
                it.copy(
                    flash = it.flash.copy(visible = false)
                )
            }
        }
    }

    // applySnapshot: Applies a successful sync payload to view state and emits story effects when needed.
    private suspend fun applySnapshot(result: StepSyncResult.Success) {
        _uiState.update {
            it.copy(
                hasPermission = true,
                dayNumber = result.snapshot.dayNumber,
                todayDistance = result.snapshot.todayDistance,
                totalDistance = result.snapshot.totalDistance,
                nextMilestoneDistance = result.snapshot.nextMilestoneDistance,
                distanceUnit = result.snapshot.distanceUnit,
                journeyAreaName = result.snapshot.currentAreaName
            )
        }

        if (result.snapshot.introUnlocked) {
            _effects.emit(JourneyEffect.PotentialIntroUnlocked)
        }
        if (result.snapshot.newUnlocksFound) {
            _effects.emit(JourneyEffect.NewStoryUnlocksFound)
        }
    }

    // sync: Runs one sync pass and updates state or feedback depending on the result type.
    private suspend fun sync(showFeedback: Boolean, trigger: StepSyncTrigger) {
        when (val result = StepSyncCoordinator.syncNow(appContext, trigger)) {
            is StepSyncResult.Success -> {
                applySnapshot(result)
                if (showFeedback) {
                    flash(ok = true, text = "Synced")
                }
            }

            StepSyncResult.NoPermission -> {
                _uiState.update { it.copy(hasPermission = false) }
                _effects.emit(JourneyEffect.PermissionsRevoked)
            }

            StepSyncResult.SkippedAlreadyRunning -> Unit

            StepSyncResult.QueuedForRerun -> Unit

            is StepSyncResult.Failure -> {
                if (showFeedback) {
                    flash(ok = false, text = "Sync failed")
                }
            }
        }
    }

    /**
     * syncOnOpenWithRetry: Retries automatic sync on open to handle short startup delays.
     * Stops as soon as success or permission revocation produces a definitive outcome.
     */
    private suspend fun syncOnOpenWithRetry() {
        var attempt = 0
        while (attempt < OPEN_SYNC_MAX_ATTEMPTS) {
            when (val result = StepSyncCoordinator.syncNow(appContext, StepSyncTrigger.AUTOMATIC)) {
                is StepSyncResult.Success -> {
                    applySnapshot(result)
                    return
                }

                StepSyncResult.NoPermission -> {
                    _uiState.update { it.copy(hasPermission = false) }
                    _effects.emit(JourneyEffect.PermissionsRevoked)
                    return
                }

                StepSyncResult.SkippedAlreadyRunning,
                StepSyncResult.QueuedForRerun,
                is StepSyncResult.Failure -> {
                    attempt += 1
                    if (attempt < OPEN_SYNC_MAX_ATTEMPTS) {
                        delay(OPEN_SYNC_RETRY_DELAY_MS)
                    }
                }
            }
        }
    }

    /**
     * loadPersistedSnapshot: Loads the latest persisted progress while sync is still warming up.
     * Avoids rendering all-zero stats when startup sync has not completed yet.
     */
    private suspend fun loadPersistedSnapshot() {
        val snapshot = journeySnapshotRepository.loadPersistedSnapshot()
        cachedSnapshot = snapshot
        _uiState.update {
            it.copy(distanceUnit = snapshot.distanceUnit)
        }

        if (!snapshot.hasProgressState) return

        _uiState.update {
            it.copy(
                dayNumber = snapshot.dayNumber,
                totalDistance = snapshot.totalDistance,
                todayDistance = snapshot.todayDistance,
                nextMilestoneDistance = snapshot.nextMilestoneDistance,
                journeyAreaName = snapshot.journeyAreaName
            )
        }
    }
}

private fun PersistedJourneySnapshot.toJourneyUiState(): JourneyUiState {
    if (!hasProgressState) {
        return JourneyUiState(distanceUnit = distanceUnit)
    }

    return JourneyUiState(
        dayNumber = dayNumber,
        todayDistance = todayDistance,
        totalDistance = totalDistance,
        nextMilestoneDistance = nextMilestoneDistance,
        distanceUnit = distanceUnit,
        journeyAreaName = journeyAreaName
    )
}

/**
 * JourneySnapshotRepository: Reads persisted journey values needed for initial screen rendering.
 * Keeps direct database access out of JourneyViewModel so screen state stays repository-driven.
 */
private class JourneySnapshotRepository(
    private val appContext: Context
) {
    suspend fun loadPersistedSnapshot(): PersistedJourneySnapshot {
        val database = DatabaseProvider.getDatabase(appContext)
        val progressState = database.progressStateDao().get()
        val settings = database.settingsDao().get()

        val distanceUnit = DistanceUnit.fromStorageValue(settings?.distanceUnit)
        if (progressState == null) {
            return PersistedJourneySnapshot(
                hasProgressState = false,
                distanceUnit = distanceUnit
            )
        }

        val totalDistance = progressState.totalDistance
        val todayDistance = database.dailyStatsDao()
            .getByDate(LocalDate.now().toString())
            ?.distanceToday
            ?: 0.0

        val milestoneDao = database.milestoneDao()
        val nextMilestone = milestoneDao.getNextUnreached(totalDistance)
        val journeyAreaName = milestoneDao.getLatestPersistentUnlockedAreaName().orEmpty()
        val progressCalculator = ProgressCalculator()

        return PersistedJourneySnapshot(
            hasProgressState = true,
            dayNumber = progressState.dayNumber,
            todayDistance = todayDistance,
            totalDistance = totalDistance,
            nextMilestoneDistance = progressCalculator.roundDistance(
                progressCalculator.getRemainingDistance(
                    currentDistance = totalDistance,
                    nextMilestoneDistance = nextMilestone?.distanceMarker ?: totalDistance
                )
            ),
            distanceUnit = distanceUnit,
            journeyAreaName = journeyAreaName
        )
    }
}

/**
 * PersistedJourneySnapshot: Bundles persisted journey fields used before sync completes.
 */
private data class PersistedJourneySnapshot(
    val hasProgressState: Boolean,
    val dayNumber: Int = 1,
    val todayDistance: Double = 0.0,
    val totalDistance: Double = 0.0,
    val nextMilestoneDistance: Double = 0.0,
    val distanceUnit: DistanceUnit = DistanceUnit.MILE,
    val journeyAreaName: String = ""
)