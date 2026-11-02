package com.olinix.audioguest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olinix.audioguest.ui.theme.Ivory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Vertical-bar sound wave. Bar heights follow a fixed pseudo-random shape (same [seed], same wave),
 * so a saved message always looks the same in the gallery and the player.
 *
 * @param animated bars breathe up and down, like the live wave while recording.
 * @param played fraction already played; bars after it use [restColor]. Negative means "all played".
 * @param level live loudness from 0 to 1 that scales the whole wave.
 */
@Composable
fun Waveform(
    color: Color,
    modifier: Modifier = Modifier,
    seed: Int = 1,
    animated: Boolean = false,
    played: Float = -1f,
    restColor: Color = Ivory.copy(alpha = 0.2f),
    barWidth: Dp = 8.dp,
    gap: Dp = 6.dp,
    level: () -> Float = { 1f },
) {
    val seconds = remember { mutableFloatStateOf(0f) }
    if (animated) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) withFrameMillis { seconds.floatValue = (it - start) / 1000f }
        }
    }

    Canvas(modifier) {
        val barPx = barWidth.toPx()
        val gapPx = gap.toPx()
        val count = max(1, floor((size.width + gapPx) / (barPx + gapPx)).toInt())
        val startX = (size.width - (count * barPx + (count - 1) * gapPx)) / 2
        val loudness = level().coerceIn(0.15f, 1f)

        for (i in 0 until count) {
            val r = random(i, seed)
            // Taller in the middle, shorter at the edges.
            val envelope = sin(PI * (i + 0.5) / count).toFloat() * 0.65f + 0.35f
            var height = max(0.07f, (0.22f + r * 0.78f) * envelope)
            if (animated) {
                val period = 0.84f + r * 1.2f
                val wave = 0.5f + 0.5f * sin(2 * PI * (seconds.floatValue / period + r)).toFloat()
                height *= (0.22f + 0.78f * wave) * loudness
            }
            val barHeight = max(barPx, size.height * height)
            val isPlayed = played < 0f || i.toFloat() / count < played
            drawRoundRect(
                color = if (isPlayed) color else restColor,
                topLeft = Offset(startX + i * (barPx + gapPx), (size.height - barHeight) / 2),
                size = Size(barPx, barHeight),
                cornerRadius = CornerRadius(barPx / 2),
            )
        }
    }
}

/** Deterministic 0..1 value per bar, the same formula the mockups use. */
private fun random(index: Int, seed: Int): Float {
    val x = sin((index + 1) * 12.9898 + seed * 78.233) * 43758.5453
    return abs(x % 1).toFloat()
}
