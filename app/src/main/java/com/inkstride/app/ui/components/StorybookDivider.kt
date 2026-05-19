package com.inkstride.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * StorybookDivider: Renders the Storybook boundary between seen and newly unlocked memories.
 */
@Composable
fun StorybookDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = color.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        PulsingFourPointStar(
            modifier = Modifier.size(10.dp),
            sizeDp = 10.dp,
            color = color
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = color.copy(alpha = 0.6f)
        )
    }
}