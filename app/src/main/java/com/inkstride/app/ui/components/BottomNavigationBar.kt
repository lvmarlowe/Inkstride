package com.inkstride.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    onJourneyClick: () -> Unit,
    onStorybookClick: () -> Unit,
    hasStorybookNotification: Boolean,
    isJourneySelected: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x0DFFFFFF))
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onJourneyClick),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Forest,
                        contentDescription = "Journey",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(42.dp)
                    )
                },
                label = "JOURNEY",
                isSelected = isJourneySelected
            )

            NavigationItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onStorybookClick),
                icon = {
                    Box {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = "Storybook",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(42.dp)
                        )

                        if (hasStorybookNotification) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp)
                                    .size(8.dp)
                                    .background(color = Color.Red, shape = CircleShape)
                            )
                        }
                    }
                },
                label = "STORYBOOK",
                isSelected = !isJourneySelected
            )
        }
    }
}

@Composable
private fun NavigationItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean
) {
    val itemColor = if (isSelected) Color.White else Color(0x99FFFFFF)

    Column(
        modifier = modifier
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides itemColor) {
            icon()
        }
        Text(
            text = label,
            color = itemColor,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.08.em
        )
    }
}