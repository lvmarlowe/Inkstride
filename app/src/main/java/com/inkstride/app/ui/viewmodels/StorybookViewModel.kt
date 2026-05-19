package com.inkstride.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.data.repositories.StorybookSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * StorybookUiState: Holds the row list for the Storybook LazyColumn along with the
 * divider index and the initial list anchor to use on first render.
 * Divider index is null when nothing new has unlocked since the last visit.
 */
data class StorybookUiState(
    val loading: Boolean = false,
    val items: List<StorybookItem> = emptyList(),
    val dividerIndex: Int? = null,
    val initialScrollIndex: Int = 0,
    val errorMessage: String? = null
)

/**
 * StorybookResolvedState: Internal resolved snapshot for a single Storybook open.
 * Keeps the exact UI state to render plus the highest already-seen distance that
 * future scroll tracking should advance from during this open.
 */
private data class StorybookResolvedState(
    val uiState: StorybookUiState,
    val startingLastSeenDistance: Double
)

/**
 * StorybookViewModel: Loads read storybook segments, computes the new-memories
 * divider position, exposes a fully resolved item list for rendering, and advances
 * the saved last-seen distance only as the user actually scrolls past segments.
 */
class StorybookViewModel(
    private val storyRepository: StoryRepository
) : ViewModel() {

    companion object {
        // Treats tiny floating-point differences as equal when comparing distance markers.
        private const val DISTANCE_COMPARISON_EPSILON = 0.0001

        // Holds the latest fully resolved Storybook state so the screen can open
        // directly into the correct content without a stale render or loading message.
        private var cachedResolvedState: StorybookResolvedState? = null

        /**
         * prepareForOpen: Resolves the exact Storybook state that should be shown on the next open
         * and stores it in the warm cache so the screen can render immediately.
         */
        suspend fun prepareForOpen(storyRepository: StoryRepository): StorybookUiState {
            val resolvedState = buildResolvedState(storyRepository)
            cachedResolvedState = resolvedState
            return resolvedState.uiState
        }

        /**
         * buildResolvedState: Computes the exact Storybook item list, divider placement,
         * and initial list anchor for the current open. Does not write progress state.
         */
        private suspend fun buildResolvedState(
            storyRepository: StoryRepository
        ): StorybookResolvedState {
            val lastSeen = storyRepository.getStorybookLastSeenDistance().coerceAtLeast(0.0)
            val segments = storyRepository
                .getUnlockedStorybookSegments()
                .map { it.copy(text = it.text.trim()) }
                .filter { it.text.isNotBlank() }

            val firstNewIndex = segments.indexOfFirst {
                (it.distanceMarker - lastSeen) > DISTANCE_COMPARISON_EPSILON
            }

            val items = buildItems(segments, firstNewIndex)
            val dividerIndex = items
                .indexOfFirst { it is StorybookItem.NewDivider }
                .takeIf { it >= 0 }

            val uiState = StorybookUiState(
                loading = false,
                items = items,
                dividerIndex = dividerIndex,
                initialScrollIndex = dividerIndex ?: items.lastIndex.coerceAtLeast(0),
                errorMessage = null
            )

            return StorybookResolvedState(
                uiState = uiState,
                startingLastSeenDistance = lastSeen
            )
        }

        /**
         * buildItems: Composes the combined LazyColumn item list with the divider injected
         * ahead of the first new segment. When firstNewIndex is negative, no divider is inserted.
         */
        private fun buildItems(
            segments: List<StorybookSegment>,
            firstNewIndex: Int
        ): List<StorybookItem> {
            if (segments.isEmpty()) return emptyList()
            if (firstNewIndex < 0) return segments.map { StorybookItem.Segment(it, isNew = false) }

            return buildList(segments.size + 1) {
                segments.forEachIndexed { index, segment ->
                    if (index == firstNewIndex) add(StorybookItem.NewDivider)
                    add(StorybookItem.Segment(segment, isNew = index >= firstNewIndex))
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(cachedResolvedState?.uiState ?: StorybookUiState())
    val uiState: StateFlow<StorybookUiState> = _uiState.asStateFlow()

    // Tracks the furthest distance already persisted during the current Storybook open.
    private var currentSessionLastSeenDistance: Double =
        cachedResolvedState?.startingLastSeenDistance ?: 0.0

    /**
     * onScreenOpened: Uses the already-prepared resolved state when available so the Storybook
     * opens directly into the correct content without a loading message or stale render.
     * If no prepared state exists, it resolves one in place without flipping to loading.
     */
    fun onScreenOpened() {
        viewModelScope.launch {
            val resolvedState = cachedResolvedState ?: runCatching {
                buildResolvedState(storyRepository)
            }.getOrElse {
                if (_uiState.value.items.isEmpty()) {
                    _uiState.value = StorybookUiState(
                        loading = false,
                        items = emptyList(),
                        dividerIndex = null,
                        initialScrollIndex = 0,
                        errorMessage = "Unable to load storybook right now."
                    )
                }
                return@launch
            }

            currentSessionLastSeenDistance = resolvedState.startingLastSeenDistance

            if (_uiState.value != resolvedState.uiState) {
                _uiState.value = resolvedState.uiState
            }
        }
    }

    /**
     * onSegmentPassed: Advances the saved Storybook last-seen distance only when the user
     * has actually scrolled far enough that a segment is completely above the viewport.
     */
    fun onSegmentPassed(distance: Double) {
        val safeDistance = distance.coerceAtLeast(0.0)
        if ((safeDistance - currentSessionLastSeenDistance) <= DISTANCE_COMPARISON_EPSILON) return

        currentSessionLastSeenDistance = safeDistance
        viewModelScope.launch {
            storyRepository.setStorybookLastSeenDistance(safeDistance)
        }
    }
}