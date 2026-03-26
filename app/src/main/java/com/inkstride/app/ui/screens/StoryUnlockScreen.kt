package com.inkstride.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.inkstride.app.data.database.DatabaseProvider
import com.inkstride.app.ui.text.StoryTextFormatter
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * StoryUnlockScreen: Displays newly unlocked story segments in a horizontal pager.
 * Fires onSegmentNavigatedAway when the user leaves a page and onContinue when they finish reading.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryUnlockScreen(
    modifier: Modifier = Modifier,
    storySegmentIds: List<Int>,
    onSegmentNavigatedAway: (Int) -> Unit,
    onContinue: (Int) -> Unit,
    showForwardArrow: Boolean = true,
    title: String = "A New Memory Made",
    subtitle: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyById = remember { mutableStateMapOf<Int, String>() }
    val areaById = remember { mutableStateMapOf<Int, String>() }
    val pagerState = rememberPagerState(pageCount = { storySegmentIds.size })

    // Loads segment text and area names from the database whenever the segment id list changes.
    LaunchedEffect(storySegmentIds) {
        val db = DatabaseProvider.getDatabase(context)
        storyById.clear()
        areaById.clear()
        storySegmentIds.forEach { segmentId ->
            val segment = db.storySegmentDao().getById(segmentId)
            storyById[segmentId] = segment?.text.orEmpty()
            areaById[segmentId] = db.milestoneDao().getAreaNameByStorySegmentId(segmentId).orEmpty()
        }
    }

    // Fires onSegmentNavigatedAway for the previous page when the settled page changes.
    LaunchedEffect(pagerState, storySegmentIds) {
        var previousPage = pagerState.settledPage
        snapshotFlow { pagerState.settledPage }.collect { currentPage ->
            if (currentPage != previousPage) {
                storySegmentIds.getOrNull(previousPage)?.let(onSegmentNavigatedAway)
                previousPage = currentPage
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Uses the provided subtitle or falls back to the current segment's area name.
                val currentSegmentId = storySegmentIds.getOrNull(pagerState.currentPage)
                val currentAreaSubtitle = subtitle ?: currentSegmentId?.let { areaById[it].orEmpty() }
                val formattedSubtitle = currentAreaSubtitle
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase(Locale.US)

                formattedSubtitle?.let {
                    Text(
                        text = it,
                        color = Color(0x8CFFFFFF),
                        fontSize = 15.sp,
                        letterSpacing = 0.1.em,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(1.dp)
                        .background(Color(0x0DFFFFFF))
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (storySegmentIds.isNotEmpty()) {
                            "${pagerState.currentPage + 1} of ${storySegmentIds.size}"
                        } else {
                            "0 of 0"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        letterSpacing = 0.08.em
                    )

                    // Renders the forward arrow only when there are more pages ahead.
                    val canGoForward = pagerState.currentPage < storySegmentIds.lastIndex
                    if (showForwardArrow && storySegmentIds.size > 1 && canGoForward) {
                        IconButton(
                            onClick = {
                                val nextPage = pagerState.currentPage + 1
                                if (nextPage <= storySegmentIds.lastIndex) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(nextPage)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Next story segment",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    // Disables swipe when the forward arrow is hidden or only one segment is present.
                    userScrollEnabled = showForwardArrow && storySegmentIds.size > 1
                ) { page ->
                    val segmentId = storySegmentIds[page]
                    Text(
                        text = StoryTextFormatter.parseItalicMarkup(storyById[segmentId].orEmpty()),
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 30.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        storySegmentIds.getOrNull(pagerState.currentPage)?.let(onContinue)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Continue")
                }
            }
        }
    }
}