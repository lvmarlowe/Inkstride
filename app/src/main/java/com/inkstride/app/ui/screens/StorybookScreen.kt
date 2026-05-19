package com.inkstride.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.inkstride.app.MainActivity
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.ui.components.StorybookDivider
import com.inkstride.app.ui.rememberViewModel
import com.inkstride.app.ui.text.StoryTextFormatter
import com.inkstride.app.ui.viewmodels.StorybookItem
import com.inkstride.app.ui.viewmodels.StorybookViewModel
import java.util.Locale
import kotlin.math.abs

/**
 * StorybookScreen: Displays all read and unlocked story segments in journey order.
 * Scrolls to the new-memories divider when segments have unlocked since the last visit;
 * otherwise opens at the end of the list.
 */
@Composable
fun StorybookScreen(
    storyRepository: StoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val activity = context as MainActivity
    val storybookViewModel = activity.rememberViewModel(storyRepository) {
        StorybookViewModel(
            storyRepository = storyRepository
        )
    }

    val uiState by storybookViewModel.uiState.collectAsState()

    // Loads the fully resolved storybook state on composition through the view model.
    LaunchedEffect(storybookViewModel) {
        storybookViewModel.onScreenOpened()
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StorybookHeader()

            Spacer(modifier = Modifier.height(16.dp))

            val errorMessage = uiState.errorMessage
            when {
                errorMessage != null -> StorybookMessage(text = errorMessage)
                uiState.loading -> StorybookMessage(text = "Loading storybook...")
                uiState.items.isEmpty() && !uiState.loading -> StorybookMessage(text = "No story segments read yet.")
                else -> {
                    key(uiState.initialScrollIndex, uiState.dividerIndex, uiState.items.size) {
                        val lazyListState = rememberLazyListState(
                            initialFirstVisibleItemIndex = uiState.initialScrollIndex
                        )
                        val density = LocalDensity.current
                        var listReady by remember { mutableStateOf(false) }

                        // Maps LazyColumn item index to segment distance so scroll tracking can
                        // persist the furthest segment whose ending has become visible.
                        val segmentDistanceByIndex = remember(uiState.items) {
                            mutableStateMapOf<Int, Double>().apply {
                                uiState.items.forEachIndexed { index, item ->
                                    if (item is StorybookItem.Segment) {
                                        put(index, item.segment.distanceMarker)
                                    }
                                }
                            }
                        }

                        // Positions the list before showing it so the screen does not briefly render a stale scroll position.
                        LaunchedEffect(uiState.items, uiState.dividerIndex) {
                            listReady = false

                            val dividerIndex = uiState.dividerIndex
                            if (dividerIndex != null) {
                                val bottomInsetPx = with(density) { 12.dp.roundToPx() }
                                repeat(3) {
                                    val layout = lazyListState.layoutInfo
                                    val dividerInfo = layout.visibleItemsInfo.firstOrNull { it.index == dividerIndex }
                                        ?: return@repeat
                                    val desiredTop = layout.viewportEndOffset - dividerInfo.size - bottomInsetPx
                                    val delta = dividerInfo.offset - desiredTop
                                    if (abs(delta) > 1) {
                                        lazyListState.scrollBy(delta.toFloat())
                                    }
                                }
                            }

                            listReady = true
                        }

                        // Persists reading progress when the bottom edge of a segment becomes visible.
                        LaunchedEffect(lazyListState, uiState.items, listReady) {
                            if (!listReady) return@LaunchedEffect

                            snapshotFlow { lazyListState.layoutInfo }
                                .collect { layoutInfo ->
                                    if (!listReady) return@collect

                                    val furthestReachedDistance = layoutInfo.visibleItemsInfo
                                        .filter { itemInfo ->
                                            segmentDistanceByIndex.containsKey(itemInfo.index) &&
                                                    (itemInfo.offset + itemInfo.size) <= layoutInfo.viewportEndOffset
                                        }
                                        .mapNotNull { itemInfo -> segmentDistanceByIndex[itemInfo.index] }
                                        .maxOrNull()

                                    if (furthestReachedDistance != null) {
                                        storybookViewModel.onSegmentPassed(furthestReachedDistance)
                                    }
                                }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .alpha(if (listReady) 1f else 0f)
                        ) {
                            itemsIndexed(
                                items = uiState.items,
                                key = { _, item ->
                                    when (item) {
                                        is StorybookItem.Segment -> "segment_${item.segment.id}"
                                        StorybookItem.NewDivider -> "new_memories_divider"
                                    }
                                }
                            ) { _, item ->
                                when (item) {
                                    is StorybookItem.Segment -> StorybookSegmentRow(item)
                                    StorybookItem.NewDivider -> StorybookDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorybookHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Storybook",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "MEMORIES MADE",
            color = Color(0x8CFFFFFF),
            fontSize = 15.sp,
            letterSpacing = 0.1.em,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x0DFFFFFF))
        )
    }
}

@Composable
private fun StorybookMessage(text: String) {
    Text(
        text = text,
        color = Color(0xB3FFFFFF),
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = (18 * 1.4).sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun StorybookSegmentRow(item: StorybookItem.Segment) {
    val segment = item.segment
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        segment.persistentAreaName
            ?.takeIf { it.isNotBlank() }
            ?.let { areaName ->
                Text(
                    text = areaName.uppercase(Locale.US),
                    color = Color(0x8CFFFFFF),
                    fontSize = 13.sp,
                    letterSpacing = 0.08.em,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

        Text(
            text = StoryTextFormatter.parseItalicMarkup(segment.text),
            color = Color(0xDEFFFFFF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = (18 * 1.5).sp
        )
    }
}