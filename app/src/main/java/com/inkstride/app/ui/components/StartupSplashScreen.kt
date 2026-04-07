// app/src/main/java/com/inkstride/app/ui/components/StartupSplashScreen.kt
package com.inkstride.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkstride.app.R

// StartupSplashScreen: Displays the branded splash loading screen only for app startup.
@Composable
fun StartupSplashScreen(modifier: Modifier = Modifier) {
    val splashFont = remember { FontFamily(Font(R.font.coming_soon, FontWeight.Normal)) }
    val logoBitmap = rememberInkstrideLogoBitmap()

    // Animates the progress bar from 0 to 100% once on composition using a single forward tween.
    val animatedProgress = remember { Animatable(0f) }
    val progress by remember { derivedStateOf { animatedProgress.value } }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Inkstride",
                color = Color.White,
                fontFamily = splashFont,
                fontSize = 48.sp,
                lineHeight = 48.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap,
                    contentDescription = "Inkstride logo",
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "Loading your journey...",
                color = Color.White,
                fontFamily = splashFont,
                fontSize = 16.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(width = 2.dp, color = Color(0xFFE6E6E6), shape = RoundedCornerShape(7.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3E3E3E),
                                    Color(0xFFCBCBCB),
                                    Color(0xFFA8A8A8)
                                )
                            )
                        )
                )
            }
        }
    }
}

// rememberInkstrideLogoBitmap: Loads the Inkstride logo from assets and returns it as an ImageBitmap, or null if not found.
@Composable
private fun rememberInkstrideLogoBitmap(): ImageBitmap? {
    val context = LocalContext.current

    return remember(context) {
        val assets = context.assets
        val candidates = buildList {
            add("inkstride_logo.png")
            addAll(assets.list("")?.filter { it.startsWith("inkstride_logo") } ?: emptyList())
        }.distinct()

        candidates.firstNotNullOfOrNull { fileName ->
            runCatching {
                assets.open(fileName).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}