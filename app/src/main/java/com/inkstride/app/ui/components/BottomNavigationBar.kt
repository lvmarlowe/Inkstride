package com.inkstride.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import com.inkstride.app.data.BadgeColor
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

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
        // Renders a faint top border to visually separate the bar from screen content.
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
                    // Constrains the box to the icon size so the badge overflow does not affect layout.
                    Box(modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = "Storybook",
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(42.dp)
                        )

                        // Renders the unread badge when unlocked story segments are waiting.
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
 * The glow has a circular base that radiates evenly around the star, with extra intensity at the points.
 * Used as an unread indicator on the Storybook tab when new story segments are available.
 */
@Composable
fun StarBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 16.dp,
    color: Color = Color.White
) {
    // Adds padding around the star so the outermost glow layers are not clipped by the canvas edge.
    val padding = sizeDp * 1.5f
    val totalSize = sizeDp + padding * 2
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "starPulse")

    // Starts at full glow, briefly contracts to 25% strength at 1200ms, then returns to full by 2400ms.
    // Ensures the glow never fully disappears at the dip by dropping to 0.25f rather than 0.0f.
    // Spends most of the ten second cycle at rest at full strength.
    val pulse = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 10000
                1.0f at 0 using LinearEasing
                0.25f at 1200 using FastOutSlowInEasing
                1.0f at 2400 using LinearOutSlowInEasing
                1.0f at 10000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseValue"
    ).value

    Canvas(modifier = modifier.size(totalSize)) {
        // Sets r as the outer point radius, cx and cy as the star center within the padded canvas.
        // Sets inner to 35% of r to produce sharp concave sides between the four points.
        val r = sizePx / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val inner = r * 0.35f

        // buildStarPath: Constructs a four-pointed star path scaled around the canvas center.
        // Places outer points every 90 degrees starting from the top; places inner points 45 degrees between them.
        fun buildStarPath(scale: Float = 1f): Path {
            val sr = r * scale
            val si = inner * scale
            return Path().apply {
                for (i in 0..3) {
                    // Subtracts 90 degrees so the first outer point starts at the top of the circle.
                    val outerAngle = Math.toRadians((i * 90.0) - 90.0).toFloat()
                    // Places the inner point 45 degrees before each outer point to form the concave sides.
                    val innerAngle = Math.toRadians((i * 90.0) - 45.0).toFloat()
                    val ox = cx + cos(outerAngle) * sr
                    val oy = cy + sin(outerAngle) * sr
                    val ix = cx + cos(innerAngle) * si
                    val iy = cy + sin(innerAngle) * si
                    if (i == 0) moveTo(ox, oy) else lineTo(ox, oy)
                    lineTo(ix, iy)
                }
                close()
            }
        }

        // Draws a circular base glow so light radiates evenly around the whole star.
        // Ties alpha to the pulse cycle by multiplying by (0.3f + pulse * 0.7f).
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = (0.18f * (0.3f + pulse * 0.7f)).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = r * 2.8f
            ),
            radius = r * 2.8f,
            center = Offset(cx, cy)
        )

        // Draws star-shaped glow layers on top to concentrate light at the points.
        // Scales each layer 30% larger than the previous to spread the glow outward from the star edge.
        // Starts alpha faint at the outermost layer and strengthens it toward the star center.
        // Keeps a minimum glow of 30% visible at the pulse dip by multiplying by (0.3f + pulse * 0.7f).
        val glowLayers = 7
        for (layer in glowLayers downTo 1) {
            val scale = 1f + (layer * 0.3f)
            val alpha = (0.07f - layer * 0.008f) * (0.3f + pulse * 0.7f)
            drawPath(
                path = buildStarPath(scale),
                color = color.copy(alpha = alpha.coerceIn(0f, 1f))
            )
        }

        // Draws the star in solid color on top of all glow layers.
        drawPath(path = buildStarPath(), color = color)
    }
}

/**
 * NavigationItem: Renders one tab with an icon and label at the selected or unselected opacity.
 * Dims unselected tabs to indicate the active screen without hiding inactive options.
 */
@Composable
private fun NavigationItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean
) {
    // Applies full white for the selected tab and reduced opacity for the unselected tab.
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