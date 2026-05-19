package com.inkstride.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import kotlin.math.cos
import kotlin.math.sin

/**
 * PulsingFourPointStar: Draws the shared four-pointed star with a soft pulsing glow.
 * Used by both Storybook tab badges and Storybook new-memories divider markers.
 */
@Composable
fun PulsingFourPointStar(
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
        val glowLayers = 7
        for (layer in glowLayers downTo 1) {
            val scale = 1f + (layer * 0.3f)
            val alpha = (0.07f - layer * 0.008f) * (0.3f + pulse * 0.7f)
            drawPath(
                path = buildStarPath(scale),
                color = color.copy(alpha = alpha.coerceIn(0f, 1f))
            )
        }

        drawPath(path = buildStarPath(), color = color)
    }
}