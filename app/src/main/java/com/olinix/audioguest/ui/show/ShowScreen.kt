package com.olinix.audioguest.ui.show

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Playback
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Surface
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatClockTime
import java.io.File

/** Darkens the top and bottom of a full-screen video so the chips and the names stay readable. */
val VideoShade = Brush.verticalGradient(
    0f to Color(0x990F0B08),
    0.2f to Color.Transparent,
    0.52f to Color.Transparent,
    1f to Color(0xEB0F0B08),
)

/**
 * Screen 14 · Show mode. A saved message plays on the tablet speaker, so the room hears it;
 * a touch anywhere starts a session. The session cuts each message after [clipSeconds].
 */
@Composable
fun ShowScreen(
    recording: Recording,
    file: File,
    clipSeconds: Int,
    event: EventInfo,
    onTap: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val isVideo = recording.mode == RecordingMode.VIDEO
    val playback = rememberPlayback(file, autoPlay = true, onPlayingChange = onPlayingChange, sound = SoundOutput.SPEAKER)

    Box(
        modifier
            .fillMaxSize()
            .screenBackground()
            .clickable(interactionSource = null, indication = null, onClick = onTap),
    ) {
        if (isVideo) {
            VideoFrame(playback, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(VideoShade))
        } else {
            Box(Modifier.fillMaxWidth().padding(bottom = 360.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                Waveform(
                    color = accent,
                    seed = recording.id.toInt(),
                    animated = playback.isPlaying,
                    gap = 7.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp).height(300.dp),
                )
            }
        }

        Column(Modifier.padding(56.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OverlayChip(R.drawable.ic_play, stringResource(R.string.show_chip), iconSize = 22.dp, bold = true)
            val type = stringResource(if (isVideo) R.string.video_message else R.string.voice_message)
            OverlayChip(if (isVideo) R.drawable.ic_video else R.drawable.ic_mic, "$type · ${formatClockTime(recording.savedAt)}")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 56.dp, end = 56.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Column {
                Text(text = event.label, style = MaterialTheme.typography.headlineMedium, fontSize = 38.sp, fontStyle = FontStyle.Italic, color = Taupe)
                Text(
                    text = event.hosts,
                    style = MaterialTheme.typography.displayLarge.copy(lineBreak = LineBreak.Heading),
                    fontSize = 80.sp,
                    lineHeight = 84.sp,
                    color = Ivory,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = Surface, modifier = Modifier.size(30.dp))
                Text(text = stringResource(R.string.show_cta), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Surface, style = MaterialTheme.typography.titleLarge)
            }
        }

        ProgressBar(
            progress = clipProgress(playback, clipSeconds),
            color = accent,
            height = 6.dp,
            track = Ivory.copy(alpha = 0.2f),
            glide = false,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 56.dp, end = 56.dp, bottom = 36.dp),
        )
    }
}

/** How much of the part that will be shown has played: long messages are cut at [clipSeconds]. */
private fun clipProgress(playback: Playback, clipSeconds: Int): Float = playback.positionMs / (clipSeconds * 1000f)

/** Translucent pill over the picture: "▶ Mensajes de los invitados". */
@Composable
fun OverlayChip(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    bold: Boolean = false,
    outlined: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(Surface.copy(alpha = 0.78f))
            .border(1.5.dp, if (outlined) accent else Ivory.copy(alpha = 0.16f), shape)
            .padding(start = 20.dp, end = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(iconSize))
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = if (bold || outlined) FontWeight.SemiBold else FontWeight.Normal,
            color = if (outlined) accent else IvorySoft,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
