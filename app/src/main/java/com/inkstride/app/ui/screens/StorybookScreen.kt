package com.inkstride.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inkstride.app.data.repository.StoryRepository
import com.inkstride.app.ui.components.NeutralLoadingScreen

@Composable
fun StorybookScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var storybookText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val storyRepository = StoryRepository(context)
        storybookText = storyRepository
            .getReadUnlockedSegments()
            .joinToString(separator = "\n\n") { it.text.trim() }
    }

    if (storybookText == null) {
        NeutralLoadingScreen(modifier = modifier)
        return
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Storybook",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (storybookText.orEmpty().isNotBlank()) {
                    storybookText.orEmpty()
                } else {
                    "No story segments read yet."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}