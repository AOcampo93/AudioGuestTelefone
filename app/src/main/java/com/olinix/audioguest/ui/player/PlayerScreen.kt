package com.olinix.audioguest.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Playback
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.OutlineCircle
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatClockTime
import com.olinix.audioguest.util.formatDuration
import java.io.File

/**
 * Screen 11 · one saved message. It starts playing on its own; voice goes to the handset
 * and video plays on screen. Previous and next move through the tab the guest came from.
 */
@Composable
fun PlayerScreen(
    state: SessionState.Player,
    recordings: List<Recording>,
    fileOf: (Recording) -> File,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = state.filter.pick(recordings)
    val index = playlist.indexOfFirst { it.id == state.recordingId }

    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .padding(start = 56.dp, end = 56.dp, top = 48.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BackPill(onClick = onBack, text = stringResource(R.string.player_back_to_gallery))
        if (index >= 0) {
            val recording = playlist[index]
            val playback = rememberPlayback(fileOf(recording), autoPlay = true, onPlayingChange = onPlayingChange)
            Heading(recording, position = index + 1, count = playlist.size)
            when (recording.mode) {
                RecordingMode.AUDIO -> VoiceBody(recording, playback, onPrevious, onNext)
                RecordingMode.VIDEO -> VideoBody(recording, playback, onPrevious, onNext)
            }
        }
    }
}

/** "🎙 Mensaje de voz · 21:42 · 2 de 6". */
@Composable
private fun Heading(recording: Recording, position: Int, count: Int) {
    val isVideo = recording.mode == RecordingMode.VIDEO
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (isVideo) R.drawable.ic_video else R.drawable.ic_mic),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        val type = stringResource(if (isVideo) R.string.video_message else R.string.voice_message)
        HeadingText(stringResource(R.string.player_heading, type, formatClockTime(recording.savedAt)))
        HeadingText("·", color = Sand)
        HeadingText(stringResource(R.string.player_position, position, count))
    }
}

@Composable
private fun HeadingText(text: String, color: Color = Taupe) {
    Text(text = text, fontSize = 24.sp, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ColumnScope.VoiceBody(recording: Recording, playback: Playback, onPrevious: () -> Unit, onNext: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterVertically),
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .border(2.dp, accent, RoundedCornerShape(percent = 50))
                .padding(start = 26.dp, end = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
            Text(
                text = stringResource(R.string.player_listen_handset),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Waveform(
            color = accent,
            seed = recording.id.toInt(),
            played = playback.progress,
            gap = 7.dp,
            modifier = Modifier.fillMaxWidth().height(300.dp),
        )
        PlaybackPosition(playback, recording.durationSeconds)
        Controls(playback, onPrevious, onNext)
    }
}

@Composable
private fun ColumnScope.VideoBody(recording: Recording, playback: Playback, onPrevious: () -> Unit, onNext: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        CameraBackdrop()
        VideoFrame(playback, modifier = Modifier.fillMaxSize())
    }
    PlaybackPosition(playback, recording.durationSeconds)
    Controls(playback, onPrevious, onNext, modifier = Modifier.padding(top = 8.dp))
}

/** 0:31 ━━━━━━──── 1:15 */
@Composable
private fun PlaybackPosition(playback: Playback, durationSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeLabel(formatDuration((playback.positionMs / 1000).toInt()))
        ProgressBar(
            progress = playback.progress,
            color = MaterialTheme.colorScheme.primary,
            height = 10.dp,
            track = Ivory.copy(alpha = 0.16f),
            glide = false,
            modifier = Modifier.weight(1f),
        )
        TimeLabel(formatDuration(durationSeconds))
    }
}

@Composable
private fun TimeLabel(text: String) {
    Text(text = text, fontSize = 22.sp, color = Ivory, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
}

/** ⏮ ⏯ ⏭ */
@Composable
private fun Controls(playback: Playback, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlineCircle(icon = R.drawable.ic_prev, contentDescription = stringResource(R.string.player_previous), onClick = onPrevious)
        AccentCircle(
            icon = if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            size = 128.dp,
            iconSize = 48.dp,
            onClick = playback::toggle,
            contentDescription = stringResource(if (playback.isPlaying) R.string.pause else R.string.play),
        )
        OutlineCircle(icon = R.drawable.ic_next, contentDescription = stringResource(R.string.player_next), onClick = onNext)
    }
}
