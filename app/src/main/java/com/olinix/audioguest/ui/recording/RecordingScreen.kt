package com.olinix.audioguest.ui.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.CameraShade
import com.olinix.audioguest.ui.components.LastSecondsBanner
import com.olinix.audioguest.ui.components.LookHereTab
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.RecordingBadge
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.countdown.CameraTabAlignment
import com.olinix.audioguest.ui.theme.Amber
import com.olinix.audioguest.ui.theme.AmberLight
import com.olinix.audioguest.ui.theme.AmberMuted
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.RecordRed
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Scrim
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatDuration

/** Seconds before the limit when the timer turns amber and the guest is asked to wrap up. */
private const val WARNING_SECONDS = 10

/**
 * Screens 5, 6 and 7 · Recording a voice or video message, including the last-10-seconds variant.
 *
 * @param level live input loudness (0..1) that moves the wave and the handset meter.
 */
@Composable
fun RecordingScreen(
    state: SessionState.Capturing,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    level: () -> Float = { 1f },
    microphone: Microphone = Microphone.HANDSET,
) {
    val lastSeconds = state.remainingSeconds <= WARNING_SECONDS
    when (state.mode) {
        RecordingMode.AUDIO -> VoiceRecording(state, lastSeconds, onFinish, level, microphone, modifier)
        RecordingMode.VIDEO -> VideoRecording(state, lastSeconds, onFinish, level, microphone, modifier)
    }
}

@Composable
private fun VoiceRecording(
    state: SessionState.Capturing,
    lastSeconds: Boolean,
    onFinish: () -> Unit,
    level: () -> Float,
    microphone: Microphone,
    modifier: Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .padding(start = 56.dp, end = 56.dp, top = 56.dp, bottom = 64.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingBadge()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.voice_message), fontSize = 22.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterVertically),
        ) {
            if (lastSeconds) LastSecondsBanner(secondsLeft = state.remainingSeconds)
            Waveform(
                color = if (lastSeconds) AmberLight else accent,
                animated = true,
                level = level,
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        TimeRow(state, lastSeconds)
        ProgressBar(
            progress = state.progress,
            color = if (lastSeconds) Amber else accent,
            track = if (lastSeconds) Amber.copy(alpha = 0.2f) else Ivory.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tablet = microphone == Microphone.TABLET
            Box(Modifier.size(56.dp).border(1.5.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(if (tablet) R.drawable.ic_tablet else R.drawable.ic_phone),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = stringResource(if (tablet) R.string.speak_into_tablet else R.string.speak_into_handset),
                fontSize = 28.sp,
                color = IvorySoft,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        FinishButton(onFinish, Modifier.padding(top = 32.dp))
    }
}

@Composable
private fun TimeRow(state: SessionState.Capturing, lastSeconds: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = formatDuration(state.elapsedSeconds),
                fontSize = 64.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (lastSeconds) AmberLight else Ivory,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            )
            Text(
                text = "/ ${formatDuration(state.maxSeconds)}",
                fontSize = 32.sp,
                color = if (lastSeconds) AmberMuted else Sand,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            text = stringResource(R.string.recording_remaining, formatDuration(state.remainingSeconds)),
            fontSize = 24.sp,
            color = if (lastSeconds) AmberLight else Sand,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun VideoRecording(
    state: SessionState.Capturing,
    lastSeconds: Boolean,
    onFinish: () -> Unit,
    level: () -> Float,
    microphone: Microphone,
    modifier: Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    // See-through: the live camera image is drawn underneath by the app root.
    Box(modifier.fillMaxSize()) {
        CameraShade()
        ViewfinderCorners()
        LookHereTab(Modifier.align(CameraTabAlignment), light = false)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, top = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RecordingBadge(onCamera = true)
                VideoClock(state, lastSeconds)
            }
            MicrophoneMeter(level, microphone)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 56.dp, end = 56.dp, bottom = 76.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (lastSeconds) LastSecondsBanner(secondsLeft = state.remainingSeconds, onCamera = true)
            FinishButton(onFinish)
        }

        ProgressBar(
            progress = state.progress,
            color = if (lastSeconds) Amber else accent,
            height = 8.dp,
            track = Ivory.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, bottom = 40.dp),
        )
    }
}

/** "0:42 / 2:00" chip over the camera image. */
@Composable
private fun VideoClock(state: SessionState.Capturing, lastSeconds: Boolean) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .height(64.dp)
            .clip(shape)
            .background(Scrim)
            .then(if (lastSeconds) Modifier.border(2.dp, Amber, shape) else Modifier)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(state.elapsedSeconds),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (lastSeconds) AmberLight else Ivory,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            text = "/ ${formatDuration(state.maxSeconds)}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (lastSeconds) AmberMuted else Sand,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Handset or tablet icon plus five tiny bars: proof that the sound comes from the chosen microphone. */
@Composable
private fun MicrophoneMeter(level: () -> Float, microphone: Microphone) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .height(64.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Scrim)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (microphone == Microphone.TABLET) R.drawable.ic_tablet else R.drawable.ic_phone),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp),
        )
        Waveform(color = accent, animated = true, level = level, barWidth = 4.dp, gap = 4.dp, modifier = Modifier.width(36.dp).height(30.dp))
    }
}

/** Rounded L-shaped marks in the four corners, like a camera viewfinder. */
@Composable
private fun ViewfinderCorners() {
    Canvas(Modifier.fillMaxSize().padding(28.dp)) {
        val arm = 64.dp.toPx()
        val radius = 22.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val color = Ivory.copy(alpha = 0.75f)
        val corner = Path().apply {
            moveTo(0f, arm)
            lineTo(0f, radius)
            quadraticTo(0f, 0f, radius, 0f)
            lineTo(arm, 0f)
        }
        // Draw the top-left mark, then mirror it into the other three corners.
        val center = Offset(size.width / 2, size.height / 2)
        for ((mirrorX, mirrorY) in listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f)) {
            scale(mirrorX, mirrorY, pivot = center) { drawPath(corner, color, style = stroke) }
        }
    }
}

/** Light "■ Terminar" button. */
@Composable
private fun FinishButton(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    PillButton(
        text = stringResource(R.string.recording_finish),
        onClick = onFinish,
        style = PillStyle.LIGHT,
        height = 120.dp,
        modifier = modifier.fillMaxWidth(),
        leading = {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(RecordRed))
        },
    )
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun VoiceRecordingPreview() {
    AudioGuestTheme { RecordingScreen(SessionState.Capturing(RecordingMode.AUDIO, 172, 180), onFinish = {}) }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun VideoRecordingPreview() {
    AudioGuestTheme {
        Box {
            CameraBackdrop()
            RecordingScreen(SessionState.Capturing(RecordingMode.VIDEO, 42, 120), onFinish = {})
        }
    }
}

