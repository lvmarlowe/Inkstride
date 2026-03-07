package com.inkstride.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

private const val PERMISSION_RECHECK_COUNT = 3
private const val PERMISSION_RECHECK_DELAY_MS = 220L

enum class AppRouteScreen {
    PERMISSIONS,
    JOURNEY,
    STORYBOOK,
    INTRO,
    STORY_UNLOCK
}

data class AppRouterUiState(
    val screen: AppRouteScreen? = null,
    val introSegmentId: Int? = null,
    val unreadUnlockSegmentIds: List<Int> = emptyList(),
    val hasStoryNotification: Boolean = false,
    val unlockAreaName: String = "",
    val returnScreenAfterStoryUnlock: AppRouteScreen = AppRouteScreen.JOURNEY
)

sealed interface AppRouterEffect {
    data object RequestBackgroundPermission : AppRouterEffect
}

class AppRouterViewModel(
    private val healthConnectManager: HealthConnectManager,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppRouterUiState())
    val uiState: StateFlow<AppRouterUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AppRouterEffect>()
    val effects: SharedFlow<AppRouterEffect> = _effects.asSharedFlow()

    private val _viewedSegmentIds = mutableSetOf<Int>()

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
                _viewedSegmentIds.clear()
                _uiState.update {
                    it.copy(
                        screen = AppRouteScreen.PERMISSIONS,
                        introSegmentId = null,
                        unreadUnlockSegmentIds = emptyList(),
                        hasStoryNotification = false,
                        unlockAreaName = ""
                    )
                }
                return@launch
            }

            healthConnectManager.onPermissionsGranted()
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
                        screen = AppRouteScreen.INTRO
                    )
                }
                return@launch
            }

            val unreadUnlockSegmentIdsFromDb = storyRepository
                .getUnlockedUnreadSegments()
                .map { segment -> segment.id }

            val unreadUnlockSegmentIds = if (
                previousScreen == AppRouteScreen.STORY_UNLOCK &&
                activeUnlockSessionIds.isNotEmpty()
            ) {
                activeUnlockSessionIds
            } else {
                unreadUnlockSegmentIdsFromDb
            }

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
                    screen = destination
                )
            }
        }
    }

    fun onBackgroundPermissionResult(hasBackgroundPermission: Boolean) {
        refreshRoute(
            hasBackgroundPermission = hasBackgroundPermission,
            afterBackgroundPermissionRequest = true
        )
    }

    fun onPermissionsResult(hasBackgroundPermission: Boolean) {
        refreshRoute(
            hasBackgroundPermission = hasBackgroundPermission,
            afterPermissionRequest = true
        )
    }

    fun openStoryDestinationFromNavigation() {
        val currentContentScreen = if (_uiState.value.screen == AppRouteScreen.STORYBOOK) {
            AppRouteScreen.STORYBOOK
        } else {
            AppRouteScreen.JOURNEY
        }

        viewModelScope.launch {
            val unreadSegments = storyRepository.getUnlockedUnreadSegments()
            if (unreadSegments.isNotEmpty()) {
                _viewedSegmentIds.clear()
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

    fun onJourneySelected() {
        _uiState.update { it.copy(screen = AppRouteScreen.JOURNEY) }
    }

    fun onIntroContinue() {
        val introSegmentId = _uiState.value.introSegmentId ?: return
        viewModelScope.launch {
            storyRepository.markAsRead(introSegmentId)
            _uiState.update { it.copy(screen = AppRouteScreen.JOURNEY, introSegmentId = null) }
        }
    }

    fun onStoryUnlockSegmentViewed(segmentId: Int) {
        _viewedSegmentIds.add(segmentId)
    }

    fun onStoryUnlockContinue() {
        viewModelScope.launch {
            _viewedSegmentIds.forEach { storyRepository.markAsRead(it) }
            _viewedSegmentIds.clear()
            val hasUnreadSegments = storyRepository.hasUnlockedUnreadSegments()
            _uiState.update {
                it.copy(
                    hasStoryNotification = hasUnreadSegments,
                    unreadUnlockSegmentIds = emptyList(),
                    unlockAreaName = "",
                    screen = it.returnScreenAfterStoryUnlock
                )
            }
        }
    }
}