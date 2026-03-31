package com.inkstride.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkstride.app.data.BadgeColor
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Defines the retry count used when permission is not immediately granted after a request.
private const val PERMISSION_RECHECK_COUNT = 3

// Sets the delay in milliseconds between each permission recheck attempt.
private const val PERMISSION_RECHECK_DELAY_MS = 220L

/**
 * AppRouteScreen: Identifies each screen the router can navigate to.
 */
enum class AppRouteScreen {
    PERMISSIONS,
    JOURNEY,
    STORYBOOK,
    INTRO,
    STORY_UNLOCK
}

/**
 * AppRouterUiState: Holds the current navigation destination and story-related display state.
 * Drives screen routing and story notification badge from a single observed state.
 */
data class AppRouterUiState(
    val screen: AppRouteScreen? = null,
    val introSegmentId: Int? = null,
    val unreadUnlockSegmentIds: List<Int> = emptyList(),
    val hasStoryNotification: Boolean = false,
    val unlockAreaName: String = "",
    val totalDistanceMiles: Double = 0.0,
    val storyBadgeColor: BadgeColor = BadgeColor.WHITE,
    val returnScreenAfterStoryUnlock: AppRouteScreen = AppRouteScreen.JOURNEY
)

/**
 * AppRouterEffect: Represents one-time side effects emitted by the router to the UI layer.
 */
sealed interface AppRouterEffect {
    data object RequestBackgroundPermission : AppRouterEffect
}

/**
 * AppRouterViewModel: Manages app-level navigation state and story unlock routing.
 * Evaluates permissions and unread story state to determine which screen to display.
 */
class AppRouterViewModel(
    appContext: Context
) : ViewModel() {

    // Stores application context-owned dependencies so the view model does not capture composable remember instances.
    private val healthConnectManager = HealthConnectManager(appContext)
    private val storyRepository = StoryRepository(appContext)

    private val _uiState = MutableStateFlow(AppRouterUiState())
    val uiState: StateFlow<AppRouterUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AppRouterEffect>()
    val effects: SharedFlow<AppRouterEffect> = _effects.asSharedFlow()

    // Exposes the owned story repository for use by screens composed within the router.
    fun getStoryRepository(): StoryRepository = storyRepository

    /**
     * refreshRoute: Recalculates the active screen based on permission state and unread story content.
     * Retries permission checks after a request to handle brief grant delays from the OS.
     * Preserves the active unlock session when refreshing from within the story unlock screen.
     */
    fun refreshRoute(
        hasBackgroundPermission: Boolean,
        afterPermissionRequest: Boolean = false,
        afterBackgroundPermissionRequest: Boolean = false
    ) {
        val previousScreen = _uiState.value.screen
        val activeUnlockSessionIds = _uiState.value.unreadUnlockSegmentIds
        _uiState.update { it.copy(screen = null) }

        viewModelScope.launch {
            var hasPermission = healthConnectManager.hasAllPermissions()
            if (afterPermissionRequest && !hasPermission) {
                repeat(PERMISSION_RECHECK_COUNT) {
                    kotlinx.coroutines.delay(PERMISSION_RECHECK_DELAY_MS)
                    hasPermission = healthConnectManager.hasAllPermissions()
                    if (hasPermission) return@repeat
                }
            }

            if (!hasPermission) {
                _uiState.update {
                    it.copy(
                        screen = AppRouteScreen.PERMISSIONS,
                        introSegmentId = null,
                        unreadUnlockSegmentIds = emptyList(),
                        hasStoryNotification = false,
                        unlockAreaName = "",
                        totalDistanceMiles = 0.0,
                        storyBadgeColor = BadgeColor.WHITE
                    )
                }
                return@launch
            }

            healthConnectManager.onPermissionsGranted()

            // Reads current progress and badge color so all route destinations receive up-to-date values.
            val totalDistanceMiles = storyRepository.getTotalDistance()
            val storyBadgeColor = storyRepository.getStoryBadgeColor()

            if (!hasBackgroundPermission && !afterBackgroundPermissionRequest) {
                _effects.emit(AppRouterEffect.RequestBackgroundPermission)
                return@launch
            }

            val intro = storyRepository.getIntroSegmentIfUnreadUnlocked()
            if (intro != null) {
                _uiState.update {
                    it.copy(
                        introSegmentId = intro.id,
                        hasStoryNotification = true,
                        totalDistanceMiles = totalDistanceMiles,
                        storyBadgeColor = storyBadgeColor,
                        screen = AppRouteScreen.INTRO
                    )
                }
                return@launch
            }

            val unreadUnlockSegmentIdsFromDb = storyRepository
                .getUnlockedUnreadSegments()
                .map { segment -> segment.id }

            // Keeps the active unlock session ids when returning to the story unlock screen mid-session.
            val unreadUnlockSegmentIds = if (
                previousScreen == AppRouteScreen.STORY_UNLOCK &&
                activeUnlockSessionIds.isNotEmpty()
            ) {
                activeUnlockSessionIds
            } else {
                unreadUnlockSegmentIdsFromDb
            }

            // Derives badge state from the active session when mid-unlock, otherwise from the database.
            val hasStoryNotification = if (
                previousScreen == AppRouteScreen.STORY_UNLOCK &&
                activeUnlockSessionIds.isNotEmpty()
            ) {
                unreadUnlockSegmentIds.isNotEmpty()
            } else {
                unreadUnlockSegmentIdsFromDb.isNotEmpty()
            }

            val unlockAreaName = unreadUnlockSegmentIds.firstOrNull()?.let { segmentId ->
                storyRepository.getAreaNameForStorySegment(segmentId)
            }.orEmpty()

            // Returns to storybook when coming from storybook, stays in unlock if segments remain, otherwise goes to journey.
            val destination = when (previousScreen) {
                AppRouteScreen.STORYBOOK -> AppRouteScreen.STORYBOOK
                AppRouteScreen.STORY_UNLOCK -> {
                    if (unreadUnlockSegmentIds.isNotEmpty()) AppRouteScreen.STORY_UNLOCK
                    else AppRouteScreen.JOURNEY
                }
                else -> AppRouteScreen.JOURNEY
            }

            _uiState.update {
                it.copy(
                    introSegmentId = null,
                    unreadUnlockSegmentIds = unreadUnlockSegmentIds,
                    hasStoryNotification = hasStoryNotification,
                    unlockAreaName = unlockAreaName,
                    totalDistanceMiles = totalDistanceMiles,
                    storyBadgeColor = storyBadgeColor,
                    screen = destination
                )
            }
        }
    }

    // onBackgroundPermissionResult: Refreshes route after the background permission dialog is dismissed.
    fun onBackgroundPermissionResult(hasBackgroundPermission: Boolean) {
        refreshRoute(
            hasBackgroundPermission = hasBackgroundPermission,
            afterBackgroundPermissionRequest = true
        )
    }

    // onPermissionsResult: Refreshes route after the Health Connect permission request returns.
    fun onPermissionsResult(hasBackgroundPermission: Boolean) {
        refreshRoute(
            hasBackgroundPermission = hasBackgroundPermission,
            afterPermissionRequest = true
        )
    }

    /**
     * openStoryDestinationFromNavigation: Routes to story unlock when unread segments exist, otherwise to storybook.
     * Records the current content screen so the router can return to it after the unlock session ends.
     */
    fun openStoryDestinationFromNavigation() {
        val currentContentScreen = if (_uiState.value.screen == AppRouteScreen.STORYBOOK) {
            AppRouteScreen.STORYBOOK
        } else {
            AppRouteScreen.JOURNEY
        }

        viewModelScope.launch {
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            if (unreadSegments.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        hasStoryNotification = true,
                        unreadUnlockSegmentIds = unreadSegments.map { segment -> segment.id },
                        returnScreenAfterStoryUnlock = currentContentScreen,
                        unlockAreaName = storyRepository.getAreaNameForStorySegment(unreadSegments.first().id),
                        screen = AppRouteScreen.STORY_UNLOCK
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        hasStoryNotification = false,
                        unreadUnlockSegmentIds = emptyList(),
                        unlockAreaName = "",
                        screen = AppRouteScreen.STORYBOOK
                    )
                }
            }
        }
    }

    // onPotentialNewUnlocks: Updates unread segment state and badge when new unlocks may be available after a sync.
    fun onPotentialNewUnlocks() {
        viewModelScope.launch {
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            if (unreadSegments.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        hasStoryNotification = true,
                        unreadUnlockSegmentIds = unreadSegments.map { segment -> segment.id },
                        unlockAreaName = storyRepository.getAreaNameForStorySegment(unreadSegments.first().id),
                        storyBadgeColor = storyRepository.getStoryBadgeColor()
                    )
                }
            }
        }
    }

    // onJourneySelected: Routes to the journey screen when the user taps the journey tab.
    fun onJourneySelected() {
        _uiState.update { it.copy(screen = AppRouteScreen.JOURNEY) }
    }

    /**
     * onIntroContinue: Marks the intro segment as read and routes to the journey screen.
     * Recalculates badge state after marking as read so the notification reflects true unread state on arrival.
     */
    fun onIntroContinue() {
        val introSegmentId = _uiState.value.introSegmentId ?: return
        viewModelScope.launch {
            storyRepository.markAsRead(introSegmentId)
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            _uiState.update {
                it.copy(
                    screen = AppRouteScreen.JOURNEY,
                    introSegmentId = null,
                    hasStoryNotification = unreadSegments.isNotEmpty(),
                    unreadUnlockSegmentIds = emptyList(),
                    unlockAreaName = ""
                )
            }
        }
    }

    // onStoryUnlockSegmentNavigatedAway: Marks a segment as read when the user swipes away from its page.
    fun onStoryUnlockSegmentNavigatedAway(segmentId: Int) {
        viewModelScope.launch {
            storyRepository.markAsRead(segmentId)
        }
    }

    // onStoryUnlockContinueFromPager: Marks the current segment as read and returns to the screen before the unlock session.
    fun onStoryUnlockContinueFromPager(currentSegmentId: Int) {
        viewModelScope.launch {
            storyRepository.markAsRead(currentSegmentId)
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            _uiState.update {
                it.copy(
                    hasStoryNotification = unreadSegments.isNotEmpty(),
                    unreadUnlockSegmentIds = emptyList(),
                    unlockAreaName = "",
                    screen = it.returnScreenAfterStoryUnlock
                )
            }
        }
    }

    // onStoryUnlockContinueFromEmptySession: Clears the unlock session without marking a segment as read and returns to the screen before the unlock session.
    fun onStoryUnlockContinueFromEmptySession() {
        viewModelScope.launch {
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            _uiState.update {
                it.copy(
                    hasStoryNotification = unreadSegments.isNotEmpty(),
                    unreadUnlockSegmentIds = emptyList(),
                    unlockAreaName = "",
                    screen = it.returnScreenAfterStoryUnlock
                )
            }
        }
    }
}