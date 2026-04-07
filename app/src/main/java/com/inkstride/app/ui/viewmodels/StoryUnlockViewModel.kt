package com.inkstride.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkstride.app.data.repositories.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StoryUnlockUiState: Holds unlocked segment content and viewed tracking for the unlock pager.
 */
data class StoryUnlockUiState(
    val loading: Boolean = false,
    val storyById: Map<Int, String> = emptyMap(),
    val areaById: Map<Int, String> = emptyMap(),
    val viewedSegmentIds: Set<Int> = emptySet()
)

/**
 * StoryUnlockViewModel: Loads unlocked segment content and tracks viewed segment ids.
 */
class StoryUnlockViewModel(
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUnlockUiState())
    val uiState: StateFlow<StoryUnlockUiState> = _uiState.asStateFlow()

    /**
     * loadSegments: Loads story text and area labels for the provided segment ids.
     * Clears existing content before fetching so stale data from a previous session does not appear while the new segments load.
     */
    fun loadSegments(storySegmentIds: List<Int>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    storyById = emptyMap(),
                    areaById = emptyMap(),
                    viewedSegmentIds = emptySet()
                )
            }

            val segments = storyRepository.getStoryUnlockSegments(storySegmentIds)
            _uiState.update {
                it.copy(
                    loading = false,
                    storyById = segments.associate { segment -> segment.id to segment.text },
                    areaById = segments.associate { segment -> segment.id to segment.areaName }
                )
            }
        }
    }

    // onSegmentViewed: Tracks segment ids viewed during the active unlock session.
    fun onSegmentViewed(segmentId: Int) {
        _uiState.update {
            it.copy(
                viewedSegmentIds = it.viewedSegmentIds + segmentId
            )
        }
    }
}