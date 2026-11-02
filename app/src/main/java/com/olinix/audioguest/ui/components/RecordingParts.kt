package com.olinix.audioguest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.ui.theme.Amber
import com.olinix.audioguest.ui.theme.AmberLight
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.RecordRed
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Scrim
import com.olinix.audioguest.ui.theme.Surface

/** Red "● Grabando" pill with a blinking dot. */
@Composable
fun RecordingBadge(modifier: Modifier = Modifier, onCamera: Boolean = false) {
    val blink = rememberInfiniteTransition(label = "recBlink").animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
        label = "recDot",
    )
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(if (onCamera) Scrim else RecordRed.copy(alpha = 0.14f))
            .border(1.5.dp, RecordRed.copy(alpha = if (onCamera) 0.7f else 0.6f), shape)
            .padding(start = 24.dp, end = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .graphicsLayer { alpha = blink.value }
                .background(RecordRed, CircleShape)
        )
        Text(
            text = stringResource(R.string.recording),
            fontSize = if (onCamera) 24.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = RecordRedLight,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Amber "8 · Quedan 8 segundos · Ve despidiéndote" warning for the last seconds. */
@Composable
fun LastSecondsBanner(secondsLeft: Int, modifier: Modifier = Modifier, onCamera: Boolean = false) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (onCamera) Scrim else Amber.copy(alpha = 0.14f))
            .border(2.dp, Amber, RoundedCornerShape(percent = 50))
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .background(Amber, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "$secondsLeft", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Surface, style = MaterialTheme.typography.titleLarge)
        }
        Column {
            Text(
                text = pluralStringResource(R.plurals.recording_seconds_left, secondsLeft, secondsLeft),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AmberLight,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.recording_wrap_up),
                fontSize = 22.sp,
                color = IvorySoft,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Tab glued to the screen edge next to the front camera: "Mira aquí →". */
@Composable
fun LookHereTab(modifier: Modifier = Modifier, light: Boolean = true) {
    val shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
    val color = if (light) Surface else IvorySoft
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (light) Ivory.copy(alpha = 0.92f) else Scrim)
            .padding(start = 24.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.look_here),
            fontSize = if (light) 22.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
    }
}

/**
 * Thin rounded progress bar.
 *
 * @param glide ease between once-per-second updates, like the recording timer.
 * Playback already updates often, so it turns this off to avoid lagging behind the sound.
 */
@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    track: Color = Ivory.copy(alpha = 0.14f),
    glide: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "progress",
    )
    val shown = if (glide) animated else target
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown)
                .clip(RoundedCornerShape(percent = 50))
                .background(color)
        )
    }
}

/** Dark placeholder where a camera image or a video is about to appear. */
@Composable
fun CameraBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(CameraPlaceholder))
}

/**
 * Darkens the live camera image so the text on top stays readable: evenly during the
 * countdown ([dimmed]), and only along the top and bottom edges while recording.
 */
@Composable
fun CameraShade(modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val shade = if (dimmed) {
        SolidColor(Charcoal.copy(alpha = 0.74f))
    } else {
        Brush.verticalGradient(
            0f to Charcoal.copy(alpha = 0.6f),
            0.16f to Color.Transparent,
            0.66f to Color.Transparent,
            1f to Charcoal.copy(alpha = 0.8f),
        )
    }
    Box(modifier.fillMaxSize().background(shade))
}

private val CameraPlaceholder = Color(0xFF302620)
