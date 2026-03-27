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

    private val _uiState = MutableStateFlow(StorybookUiState())
    val uiState: StateFlow<StorybookUiState> = _uiState.asStateFlow()

    // onScreenOpened: Loads read unlocked storybook segments and maps display-safe values.
    fun onScreenOpened() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            runCatching {
                storyRepository
                    .getReadUnlockedStorybookSegments()
                    .map { it.copy(text = it.text.trim()) }
                    .filter { it.text.isNotBlank() }
            }.onSuccess { segments ->
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
                        segments = emptyList(),
                        errorMessage = "Unable to load storybook right now."
                    )
                }
            }
        }
    }
}