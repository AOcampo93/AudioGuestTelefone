package com.olinix.audioguest.ui.countdown

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.InfoChip
import com.olinix.audioguest.ui.components.LookHereTab
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft

/** Vertical position of the front camera on the Tab M11 held in portrait (18% from the top). */
val CameraTabAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = -0.64f)

/** Screen 4 · Full-screen 3, 2, 1 and "¡Ahora!". The recording starts by itself afterwards. */
@Composable
fun CountdownScreen(state: SessionState.Countdown, modifier: Modifier = Modifier) {
    val isVideo = state.mode == RecordingMode.VIDEO

    Box(modifier.fillMaxSize().screenBackground()) {
        if (isVideo) {
            CameraBackdrop(dimmed = true)
            LookHereTab(Modifier.align(CameraTabAlignment))
        }

        InfoChip(
            text = stringResource(if (isVideo) R.string.video_message else R.string.voice_message),
            icon = if (isVideo) R.drawable.ic_video else R.drawable.ic_mic,
            modifier = Modifier.padding(56.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(560.dp), contentAlignment = Alignment.Center) {
                if (state.number > 0) Number(state.number) else Now()
            }
            Text(
                text = stringResource(
                    when {
                        state.number > 0 && isVideo -> R.string.countdown_look_at_camera
                        state.number > 0 -> R.string.countdown_get_ready
                        isVideo -> R.string.countdown_now_video
                        else -> R.string.speak_into_handset
                    }
                ),
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                color = IvorySoft,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(max = 620.dp),
            )
        }
    }
}

/** A big number inside a ring that empties during its second. */
@Composable
private fun BoxScope.Number(number: Int) {
    val accent = MaterialTheme.colorScheme.primary
    val remaining = remember(number) { Animatable(1f) }
    LaunchedEffect(number) {
        remaining.animateTo(0f, tween(durationMillis = 1_000, easing = LinearEasing))
    }

    Canvas(Modifier.matchParentSize()) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawCircle(Ivory.copy(alpha = 0.14f), radius = size.minDimension / 2 - inset, style = Stroke(stroke))
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 360f * remaining.value,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    Text(
        text = "$number",
        // Cormorant defaults to old-style figures; the countdown needs full-height ones.
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum"),
        fontSize = 420.sp,
        lineHeight = 420.sp,
        color = Ivory,
        // Cormorant's figures sit low in the line box; lift them to the optical center.
        modifier = Modifier.offset(y = (-30).dp),
    )
}

/** "¡Ahora!" pops in inside a glowing ring. */
@Composable
private fun BoxScope.Now() {
    val accent = MaterialTheme.colorScheme.primary
    val scale = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = ((scale.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
            }
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        0.6f to accent.copy(alpha = 0.22f),
                        1f to Color.Transparent,
                        radius = size.minDimension * 0.8f,
                    ),
                    radius = size.minDimension * 0.8f,
                )
            }
            .border(3.dp, accent, CircleShape)
    )
    Text(
        text = stringResource(R.string.countdown_now),
        style = MaterialTheme.typography.displayLarge,
        fontSize = 168.sp,
        lineHeight = 168.sp,
        fontStyle = FontStyle.Italic,
        color = accent,
        maxLines = 1,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun CountdownPreview() {
    AudioGuestTheme { CountdownScreen(SessionState.Countdown(RecordingMode.VIDEO, 2)) }
}
