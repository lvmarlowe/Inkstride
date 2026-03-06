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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.inkstride.app.data.repositories.StoryRepository
import com.inkstride.app.ui.components.NeutralLoadingScreen
import java.util.Locale

@Composable
fun StorybookScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var readSegments by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(Unit) {
        val storyRepository = StoryRepository(context)
        readSegments = storyRepository
            .getReadUnlockedSegments()
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
    }

    val segments = readSegments
    if (segments == null) {
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
                    text = ("MEMORIES MADE" +
                            "").uppercase(Locale.US),
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
                if (segments.isEmpty()) {
                    Text(
                        text = "No story segments read yet.",
                        color = Color(0xB3FFFFFF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = (18 * 1.4).sp
                    )
                } else {
                    segments.forEachIndexed { index, segment ->
                        Text(
                            text = segment,
                            color = Color(0xB3FFFFFF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = (18 * 1.4).sp
                        )

                        if (index < segments.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}