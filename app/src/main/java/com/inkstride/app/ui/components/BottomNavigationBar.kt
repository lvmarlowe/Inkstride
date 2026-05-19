package com.inkstride.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.inkstride.app.data.BadgeColor

// Defines the gold color value used for the earned story badge.
private val GoldBadgeColor = Color(0xFFFDB515)

/**
 * BottomNavigationBar: Renders the two-tab bottom navigation bar for Journey and Storybook screens.
 * Shows a subtle pulsing star badge on the Storybook tab when unread story segments are available.
 */
@Composable
fun BottomNavigationBar(
    onJourneyClick: () -> Unit,
    onStorybookClick: () -> Unit,
    hasStorybookNotification: Boolean,
    modifier: Modifier = Modifier,
    isJourneySelected: Boolean = true,
    storyBadgeColor: BadgeColor = BadgeColor.WHITE
) {
    // Resolves the BadgeColor enum to a Compose Color for use in the star badge.
    val badgeColor = when (storyBadgeColor) {
        BadgeColor.WHITE -> Color.White
        BadgeColor.GOLD -> GoldBadgeColor
    }

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
                    Box(modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = "Storybook",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(42.dp)
                        )

                        if (hasStorybookNotification) {
                            StarBadge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 16.dp, y = (-14).dp),
                                sizeDp = 16.dp,
                                color = badgeColor
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

/**
 * StarBadge: Draws a four-pointed star with a permanent soft glow that briefly dims every ten seconds.
 * Used as an unread indicator on the Storybook tab when new story segments are available.
 */
@Composable
fun StarBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 16.dp,
    color: Color = Color.White
) {
    PulsingFourPointStar(
        modifier = modifier,
        sizeDp = sizeDp,
        color = color
    )
}

@Composable
private fun NavigationItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean
) {
    val itemColor = if (isSelected) Color.White else Color(0x66FFFFFF)

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