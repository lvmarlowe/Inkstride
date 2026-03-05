package com.inkstride.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BottomNavigationContentHeight = 60.dp
private val BottomNavigationBottomSpacing = 20.dp

@Composable
fun BottomNavigationBar(
    onJourneyClick: () -> Unit,
    onStorybookClick: () -> Unit,
    hasStorybookNotification: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomNavigationContentHeight)
                .padding(bottom = BottomNavigationBottomSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onJourneyClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Forest,
                    contentDescription = "Journey",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onStorybookClick),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = "Storybook",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )

                    if (hasStorybookNotification) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 2.dp)
                                .size(10.dp)
                                .background(color = Color.Red, shape = CircleShape)
                        )
                    }
                }
            }
        }
    }
}