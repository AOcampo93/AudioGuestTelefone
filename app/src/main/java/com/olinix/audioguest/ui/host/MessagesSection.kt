package com.olinix.audioguest.ui.host

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.audio.rememberVideoFrame
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.theme.AmberLight
import com.olinix.audioguest.ui.theme.DialogScrim
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatDayAndTime
import com.olinix.audioguest.util.formatDuration
import java.io.File

/** What the "Mensajes" section lists and can do, bundled so the panel's signature stays short. */
class MessageActions(
    /** Every saved message, newest first, including the kinds guests cannot see. */
    val recordings: List<Recording>,
    val fileOf: (Recording) -> File,
    val onDelete: (Long) -> Unit,
)

/**
 * "Mensajes": every saved message as a row that can be played or deleted. Built as list items,
 * so only the rows on screen load their video frame, however long the event's list grows.
 */
fun LazyListScope.messageItems(
    messages: MessageActions,
    settings: HostSettings,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit,
) {
    val recordings = messages.recordings
    item {
        val videos = recordings.count { it.mode == RecordingMode.VIDEO }
        Column(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = pluralStringResource(R.plurals.host_messages_total, recordings.size, recordings.size) + " · " +
                    stringResource(R.string.host_messages_split, videos, recordings.size - videos),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ivory,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.host_messages_hint), fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (recordings.isEmpty()) {
        item {
            PanelCard {
                Text(stringResource(R.string.host_messages_empty), fontSize = 22.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    items(recordings, key = { it.id }) { recording ->
        MessageRow(
            recording = recording,
            file = messages.fileOf(recording),
            hidden = !settings.allows(recording.mode),
            onPlay = { onPlay(recording) },
            onDelete = { onDelete(recording) },
        )
    }
}

/** [video frame or mic] Mensaje de video · 3 nov · 21:42 · 0:35  [▶] [🗑] */
@Composable
private fun MessageRow(recording: Recording, file: File, hidden: Boolean, onPlay: () -> Unit, onDelete: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceRaised)
            .clickable(onClick = onPlay)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 96.dp, height = 128.dp).clip(RoundedCornerShape(16.dp)).background(Ivory.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            if (recording.mode == RecordingMode.VIDEO) {
                rememberVideoFrame(file)?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            } else {
                Box(Modifier.size(56.dp).border(1.5.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(if (recording.mode == RecordingMode.VIDEO) R.string.host_messages_video else R.string.host_messages_voice),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ivory,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${formatDayAndTime(recording.savedAt)} · ${formatDuration(recording.durationSeconds)}",
                fontSize = 20.sp,
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hidden) {
                Text(stringResource(R.string.host_messages_hidden), fontSize = 18.sp, color = AmberLight, style = MaterialTheme.typography.bodyMedium)
            }
        }
        AccentCircle(icon = R.drawable.ic_play, size = 72.dp, iconSize = 28.dp, onClick = onPlay, contentDescription = stringResource(R.string.host_messages_play))
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.5.dp, RecordRedLight.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_trash), contentDescription = stringResource(R.string.host_messages_delete), tint = RecordRedLight, modifier = Modifier.size(28.dp))
        }
    }
}

/** A message playing over the panel, out loud through the tablet's speaker since the host stands in front of it. */
@Composable
fun MessagePlayerOverlay(recording: Recording, file: File, onClose: () -> Unit, onPlayingChange: (Boolean) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val playback = rememberPlayback(file, autoPlay = true, onPlayingChange = onPlayingChange, sound = SoundOutput.SPEAKER)
    val isVideo = recording.mode == RecordingMode.VIDEO
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DialogScrim)
            // Swallow taps on the scrim so nothing underneath reacts.
            .clickable(interactionSource = null, indication = null, onClick = {})
            .padding(horizontal = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(SurfaceRaised)
                .border(1.5.dp, Ivory.copy(alpha = 0.14f), RoundedCornerShape(36.dp))
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (isVideo) R.string.host_messages_video else R.string.host_messages_voice),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ivory,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(formatDayAndTime(recording.savedAt), fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
                }
                PillButton(
                    text = stringResource(R.string.host_messages_close),
                    onClick = onClose,
                    style = PillStyle.OUTLINE,
                    height = 64.dp,
                    borderColor = Ivory.copy(alpha = 0.3f),
                )
            }
            if (isVideo) {
                Box(Modifier.fillMaxWidth().height(720.dp).clip(RoundedCornerShape(24.dp))) {
                    CameraBackdrop()
                    VideoFrame(playback, Modifier.fillMaxSize())
                }
            } else {
                Waveform(
                    color = accent,
                    seed = recording.id.toInt(),
                    played = playback.progress,
                    gap = 6.dp,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration((playback.positionMs / 1000).toInt()), fontSize = 20.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
                ProgressBar(
                    progress = playback.progress,
                    color = accent,
                    height = 8.dp,
                    track = Ivory.copy(alpha = 0.16f),
                    glide = false,
                    modifier = Modifier.weight(1f),
                )
                Text(formatDuration(recording.durationSeconds), fontSize = 20.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
            }
            AccentCircle(
                icon = if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                size = 96.dp,
                iconSize = 38.dp,
                onClick = playback::toggle,
                contentDescription = stringResource(if (playback.isPlaying) R.string.host_messages_pause else R.string.host_messages_play),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
