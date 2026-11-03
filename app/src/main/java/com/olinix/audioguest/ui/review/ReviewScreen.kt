package com.olinix.audioguest.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.ConfirmDialog
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatDuration
import java.io.File

/** Screen 8 · "¿Te gusta cómo quedó?" Save, retry or discard (with confirmation). */
@Composable
fun ReviewScreen(
    state: SessionState.Review,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDiscard by remember { mutableStateOf(false) }
    val isVideo = state.mode == RecordingMode.VIDEO

    Box(modifier.fillMaxSize().screenBackground()) {
        Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 72.dp, bottom = 64.dp)) {
            Text(
                text = stringResource(R.string.review_title),
                style = MaterialTheme.typography.displayMedium,
                color = Ivory,
            )
            Text(
                text = stringResource(if (isVideo) R.string.review_watch_first else R.string.review_listen_first),
                style = MaterialTheme.typography.bodyMedium,
                color = Sand,
                modifier = Modifier.padding(top = 10.dp),
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isVideo) {
                    VideoPreview(state.file, state.durationSeconds, onPlayingChange)
                } else {
                    VoicePreview(state.file, state.durationSeconds, onPlayingChange)
                }
            }

            PillButton(
                text = stringResource(R.string.review_save),
                onClick = onSave,
                icon = R.drawable.ic_check,
                height = 128.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PillButton(
                    text = stringResource(R.string.review_retry),
                    onClick = onRetry,
                    style = PillStyle.OUTLINE,
                    icon = R.drawable.ic_retry,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = stringResource(R.string.review_discard),
                    onClick = { confirmingDiscard = true },
                    style = PillStyle.QUIET,
                    icon = R.drawable.ic_trash,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (confirmingDiscard) {
            ConfirmDialog(
                icon = R.drawable.ic_trash,
                title = stringResource(R.string.discard_title),
                body = stringResource(R.string.discard_body),
                keepText = stringResource(R.string.discard_keep),
                confirmText = stringResource(R.string.discard_confirm),
                onKeep = { confirmingDiscard = false },
                onConfirm = onDiscard,
            )
        }
    }
}

/** Voice preview: the wave fills in as the message plays through the handset. */
@Composable
private fun VoicePreview(file: File, durationSeconds: Int, onPlayingChange: (Boolean) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val playback = rememberPlayback(file, onPlayingChange = onPlayingChange)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceRaised)
            .border(1.5.dp, Ivory.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            .padding(40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.review_your_voice), fontSize = 22.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
        }
        Waveform(color = accent, played = playback.progress, modifier = Modifier.fillMaxWidth().height(160.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentCircle(
                icon = if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                size = 108.dp,
                iconSize = 44.dp,
                onClick = playback::toggle,
            )
            PlaybackTime(positionSeconds = (playback.positionMs / 1000).toInt(), durationSeconds = durationSeconds)
        }
    }
}

/** Video preview: the recording itself, played on screen. A tap anywhere on it plays or pauses. */
@Composable
private fun VideoPreview(file: File, durationSeconds: Int, onPlayingChange: (Boolean) -> Unit) {
    val playback = rememberPlayback(file, onPlayingChange = onPlayingChange)
    Box(
        Modifier
            .fillMaxWidth()
            .height(640.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = playback::toggle),
    ) {
        CameraBackdrop()
        VideoFrame(playback, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Charcoal.copy(alpha = 0.85f))))
        // The big button only shows while paused, so it never covers the guest's face.
        if (!playback.isPlaying) {
            AccentCircle(
                icon = R.drawable.ic_play,
                size = 132.dp,
                iconSize = 52.dp,
                contentDescription = stringResource(R.string.play),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatDuration((playback.positionMs / 1000).toInt()), fontSize = 22.sp, color = Ivory, style = MaterialTheme.typography.bodyMedium)
            ProgressBar(
                progress = playback.progress,
                color = MaterialTheme.colorScheme.primary,
                height = 8.dp,
                track = Ivory.copy(alpha = 0.25f),
                glide = false,
                modifier = Modifier.weight(1f),
            )
            Text(formatDuration(durationSeconds), fontSize = 22.sp, color = Ivory, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlaybackTime(positionSeconds: Int, durationSeconds: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(formatDuration(positionSeconds), fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = Ivory, style = MaterialTheme.typography.titleLarge)
        Text(" / ${formatDuration(durationSeconds)}", fontSize = 34.sp, fontWeight = FontWeight.Medium, color = Sand, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun ReviewPreview() {
    AudioGuestTheme { ReviewScreen(SessionState.Review(RecordingMode.AUDIO, 52, File("")), onSave = {}, onRetry = {}, onDiscard = {}, onPlayingChange = {}) }
}
