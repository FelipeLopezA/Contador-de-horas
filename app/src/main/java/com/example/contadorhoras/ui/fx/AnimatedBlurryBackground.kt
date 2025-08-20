package com.example.contadorhoras.ui.fx

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AnimatedBlurryBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    blurRadius: Dp = 60.dp,      // intensidad del desenfoque
    globalAlpha: Float = 0.85f,   // transparencia general
    speedMs: Int = 12_000         // velocidad del movimiento
) {
    val infinite = rememberInfiniteTransition(label = "bg")

    // Estados iniciales de blobs
    val blobs = remember(colors.size) {
        List(colors.size) {
            BlobState(
                x0 = Random.nextFloat().coerceIn(0.15f, 0.85f),
                y0 = Random.nextFloat().coerceIn(0.15f, 0.85f),
                r0 = Random.nextFloat().coerceIn(0.18f, 0.38f)
            )
        }
    }

    // Animaciones por blob
    val anims = blobs.mapIndexed { i, b ->
        val x by infinite.animateFloat(
            initialValue = b.x0,
            targetValue = Random.nextFloat().coerceIn(0.15f, 0.85f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = speedMs + i * 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "x$i"
        )
        val y by infinite.animateFloat(
            initialValue = b.y0,
            targetValue = Random.nextFloat().coerceIn(0.15f, 0.85f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = speedMs + i * 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "y$i"
        )
        val r by infinite.animateFloat(
            initialValue = b.r0,
            targetValue = Random.nextFloat().coerceIn(0.20f, 0.42f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = speedMs + i * 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "r$i"
        )
        Triple(x, y, r)
    }

    // Dibujamos y aplicamos blur al resultado
    Box(modifier = modifier.fillMaxSize().blur(blurRadius)) {
        Canvas(Modifier.fillMaxSize()) {
            drawBlobs(anims, colors, globalAlpha)
        }
    }
}

private fun DrawScope.drawBlobs(
    anims: List<Triple<Float, Float, Float>>,
    colors: List<Color>,
    globalAlpha: Float
) {
    val w = size.width
    val h = size.height
    anims.forEachIndexed { idx, (x, y, r) ->
        val cx = x * w
        val cy = y * h
        val radius = r * minOf(w, h)
        val c = colors[idx % colors.size]

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    c.copy(alpha = 0.75f * globalAlpha),
                    c.copy(alpha = 0f)
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            center = Offset(cx, cy),
            radius = radius
        )
    }
}

private data class BlobState(val x0: Float, val y0: Float, val r0: Float)
