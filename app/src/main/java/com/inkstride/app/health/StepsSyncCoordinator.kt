package com.inkstride.app.health

import android.content.Context
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.data.DistanceUnit
import com.inkstride.app.data.repositories.ProgressRepository
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.services.AppErrorHandler
import com.inkstride.app.services.MilestoneEngine
import com.inkstride.app.services.ProgressCalculator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * StepSyncCoordinator: Coordinates step sync execution and prevents overlapping sync runs.
 * Queues manual retriggers so no user-initiated sync is silently dropped.
 */
object StepSyncCoordinator {

    // Guards isSyncRunning and pendingManualRerun so concurrent sync calls stay consistent.
    private val stateMutex = Mutex()

    // Tracks whether a sync is currently running to block duplicate executions.
    private var isSyncRunning = false

    // Flags a pending manual rerun requested while a sync was already running.
    private var pendingManualRerun = false

    // Wraps sync execution with error handling and retry logic.
    private val errorHandler = AppErrorHandler()

    /**
     * syncNow: Runs a step sync for the given trigger type and returns the result.
     * Queues manual triggers when a sync is already running so they are not lost.
     * Skips automatic and background triggers when a sync is already running.
     */
    suspend fun syncNow(
        context: Context,
        trigger: StepSyncTrigger = StepSyncTrigger.AUTOMATIC
    ): StepSyncResult {
        val immediateResult = stateMutex.withLock {
            if (isSyncRunning) {
                when (trigger) {
                    StepSyncTrigger.MANUAL -> {
                        pendingManualRerun = true
                        StepSyncResult.QueuedForRerun
                    }

                    StepSyncTrigger.AUTOMATIC,
                    StepSyncTrigger.BACKGROUND -> StepSyncResult.SkippedAlreadyRunning
                }
            } else {
                isSyncRunning = true
                null
            }
        }

        if (immediateResult != null) {
            return immediateResult
        }

        var latestResult: StepSyncResult
        while (true) {
            latestResult = runSingleSync(context)

            val shouldRunAgain = stateMutex.withLock {
                if (pendingManualRerun) {
                    pendingManualRerun = false
                    true
                } else {
                    isSyncRunning = false
                    false
                }
            }

            if (!shouldRunAgain) {
                return latestResult
            }
        }
    }

    /**
     * runSingleSync: Executes one full sync pass and returns the result.
     * Reads steps, persists progress, checks milestone unlocks, and assembles the snapshot.
     */
    private suspend fun runSingleSync(context: Context): StepSyncResult {
        val outcome = errorHandler.runSuspend(shouldRetry = true) {
            val totals = StepsSyncer.syncIfPermitted(context)
            if (totals == null) {
                StepSyncResult.NoPermission
            } else {
                val healthConnectManager = HealthConnectManager(context)
                val journeyStart = healthConnectManager.getJourneyStartInstant()
                val dayNumber = HealthConnectManager.computeDayNumberFromJourneyStart(journeyStart)

                val database = DatabaseProvider.getDatabase(context)
                val progressRepository = ProgressRepository(
                    progressStateDao = database.progressStateDao(),
                    dailyStatsDao = database.dailyStatsDao()
                )
                val progressCalculator = ProgressCalculator()

                val totalDistance = progressRepository.persistSnapshotFromHealthConnect(
                    stepTotals = totals,
                    dayNumber = dayNumber
                )

                val milestoneEngine = MilestoneEngine(context)
                milestoneEngine.checkAndUnlockForDistance(totalDistance)

                val milestoneDao = database.milestoneDao()
                val nextMilestone = milestoneDao.getNextUnreached(totalDistance)
                val latestPersistentUnlockedAreaName =
                    milestoneDao.getLatestPersistentUnlockedAreaName().orEmpty()
                val nextMilestoneDistance = progressCalculator.roundDistance(
                    progressCalculator.getRemainingDistance(
                        currentDistance = totalDistance,
                        nextMilestoneDistance = nextMilestone?.distanceMarker ?: totalDistance
                    )
                )

                val rawDistanceUnit = database.settingsDao().get()?.distanceUnit
                val distanceUnit = DistanceUnit.fromStorageValue(rawDistanceUnit)

                val storyRepository = StoryRepository(context)
                val introUnlocked = storyRepository.getIntroSegmentIfUnreadUnlocked() != null
                val newUnlocksFound = storyRepository.hasUnlockedUnreadSegments()

                StepSyncResult.Success(
                    snapshot = StepSyncSnapshot(
                        dayNumber = dayNumber,
                        todayDistance = progressCalculator.roundDistance(
                            progressCalculator.stepsToDistance(totals.todaySteps)
                        ),
                        totalDistance = totalDistance,
                        nextMilestoneDistance = nextMilestoneDistance,
                        distanceUnit = distanceUnit,
                        introUnlocked = introUnlocked,
                        newUnlocksFound = newUnlocksFound,
                        currentAreaName = latestPersistentUnlockedAreaName
                    )
                )
            }
        }

        return when (outcome) {
            is AppErrorHandler.Outcome.Success -> outcome.value
            is AppErrorHandler.Outcome.Failure -> StepSyncResult.Failure(outcome.shouldRetry)
        }
    }
}

/**
 * StepSyncTrigger: Identifies what initiated a sync so the coordinator can apply the right queuing behavior.
 */
enum class StepSyncTrigger {
    MANUAL,
    AUTOMATIC,
    BACKGROUND
}

/**
 * StepSyncSnapshot: Stores the output of a successful sync pass for use by the UI layer.
 * Carries distance, milestone, story, and unit data assembled in one object.
 */
data class StepSyncSnapshot(
    val dayNumber: Int,
    val todayDistance: Double,
    val totalDistance: Double,
    val nextMilestoneDistance: Double,
    val distanceUnit: DistanceUnit,
    val introUnlocked: Boolean,
    val newUnlocksFound: Boolean,
    val currentAreaName: String
)

/**
 * StepSyncResult: Represents all possible outcomes of a sync attempt.
 * Lets callers handle each case without inspecting raw exceptions or error codes.
 */
sealed interface StepSyncResult {
    data class Success(val snapshot: StepSyncSnapshot) : StepSyncResult
    data object NoPermission : StepSyncResult
    data object SkippedAlreadyRunning : StepSyncResult
    data object QueuedForRerun : StepSyncResult
    data class Failure(val shouldRetry: Boolean) : StepSyncResult
}