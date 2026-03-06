package com.inkstride.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inkstride.app.data.database.DatabaseProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryUnlockScreen(
    modifier: Modifier = Modifier,
    storySegmentIds: List<Int>,
    onSegmentViewed: (Int) -> Unit,
    onContinue: () -> Unit,
    showForwardArrow: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storyById = remember { mutableStateMapOf<Int, String>() }
    val viewedSegmentIds = remember { mutableSetOf<Int>() }
    val pagerState = rememberPagerState(pageCount = { storySegmentIds.size })

    LaunchedEffect(storySegmentIds) {
        val db = DatabaseProvider.getDatabase(context)
        storyById.clear()
        storySegmentIds.forEach { segmentId ->
            val segment = db.storySegmentDao().getById(segmentId)
            storyById[segmentId] = segment?.text.orEmpty()
        }
    }

    LaunchedEffect(pagerState.currentPage, storySegmentIds) {
        val currentSegmentId = storySegmentIds.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (viewedSegmentIds.add(currentSegmentId)) {
            onSegmentViewed(currentSegmentId)
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Story",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

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
                                imageVector = Icons.Rounded.ArrowForward,
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
                        .weight(1f, fill = false),
                    userScrollEnabled = showForwardArrow && storySegmentIds.size > 1
                ) { page ->
                    val segmentId = storySegmentIds[page]
                    Text(
                        text = storyById[segmentId].orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onContinue,
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