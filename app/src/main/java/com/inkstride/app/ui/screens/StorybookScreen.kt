package com.inkstride.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.inkstride.app.MainActivity
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.ui.components.NeutralLoadingScreen
import com.inkstride.app.ui.rememberViewModel
import com.inkstride.app.ui.text.StoryTextFormatter
import com.inkstride.app.ui.viewmodels.StorybookViewModel
import java.util.Locale

/**
 * StorybookScreen: Displays all read and unlocked story segments in journey order.
 * Shows a loading screen until segments are fetched, then renders each entry with its area label.
 */
@Composable
fun StorybookScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val activity = context as MainActivity
    val storyRepository = remember { StoryRepository(context) }
    val storybookViewModel = activity.rememberViewModel(storyRepository) {
        StorybookViewModel(
            storyRepository = storyRepository
        )
    }

    val uiState by storybookViewModel.uiState.collectAsState()

    // Loads read unlocked segments on composition through the view model.
    LaunchedEffect(storybookViewModel) {
        storybookViewModel.onScreenOpened()
    }

    if (uiState.loading) {
        NeutralLoadingScreen(modifier = modifier)
        return
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val errorMessage = uiState.errorMessage
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color(0xB3FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = (18 * 1.4).sp
                    )
                } else if (uiState.segments.isEmpty()) {
                    Text(
                        text = "No story segments read yet.",
                        color = Color(0xB3FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = (18 * 1.4).sp
                    )
                } else {
                    uiState.segments.forEachIndexed { index, segment ->
                        // Renders the persistent area label above the segment text when present.
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
                            color = Color(0xB3FFFFFF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = (18 * 1.4).sp
                        )

                        // Adds spacing between segments but omits it after the last entry.
                        if (index < uiState.segments.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}