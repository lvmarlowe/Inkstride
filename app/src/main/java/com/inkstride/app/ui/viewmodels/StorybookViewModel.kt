package com.inkstride.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.data.repositories.StorybookSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StorybookUiState: Represents storybook loading, content, and failure display states.
 */
data class StorybookUiState(
    val loading: Boolean = true,
    val segments: List<StorybookSegment> = emptyList(),
    val errorMessage: String? = null
)

/**
 * StorybookViewModel: Loads read storybook segments and exposes state-driven rendering data.
 */
class StorybookViewModel(
    private val storyRepository: StoryRepository
) : ViewModel() {

    companion object {
        // Holds pre-loaded storybook segments so the screen renders without a loading state on first open.
        private var cachedSegments: List<StorybookSegment>? = null

        // warmCache: Loads and caches storybook segments during startup so the screen opens without delay.
        suspend fun warmCache(storyRepository: StoryRepository) {
            if (cachedSegments != null) return

            cachedSegments = storyRepository
                .getReadUnlockedStorybookSegments()
                .map { it.copy(text = it.text.trim()) }
                .filter { it.text.isNotBlank() }
        }
    }

    private val _uiState = MutableStateFlow(
        StorybookUiState(
            loading = cachedSegments == null,
            segments = cachedSegments.orEmpty(),
            errorMessage = null
        )
    )
    val uiState: StateFlow<StorybookUiState> = _uiState.asStateFlow()

    // onScreenOpened: Loads read unlocked storybook segments and maps display-safe values.
    fun onScreenOpened() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    loading = currentState.segments.isEmpty(),
                    errorMessage = null
                )
            }
            runCatching {
                storyRepository
                    .getReadUnlockedStorybookSegments()
                    .map { it.copy(text = it.text.trim()) }
                    .filter { it.text.isNotBlank() }
            }.onSuccess { segments ->
                cachedSegments = segments
                _uiState.update {
                    it.copy(
                        loading = false,
                        segments = segments,
                        errorMessage = null
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = if (it.segments.isEmpty()) {
                            "Unable to load storybook right now."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}