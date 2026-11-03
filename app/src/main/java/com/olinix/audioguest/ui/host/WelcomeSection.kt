package com.olinix.audioguest.ui.host

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.audio.rememberVideoFrame
import com.olinix.audioguest.audio.videoSeconds
import com.olinix.audioguest.session.HostVideoFile
import com.olinix.audioguest.session.VideoProblem
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Scrim
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe
import com.olinix.audioguest.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a section built around one of the host's videos shows and can do, bundled so signatures stay short. */
class HostVideoActions(
    val video: HostVideoFile?,
    val problem: VideoProblem?,
    val maxSeconds: Int,
    val onRecord: () -> Unit,
    val onImport: (Uri) -> Unit,
    val onDelete: () -> Unit,
    val onPlayingChange: (Boolean) -> Unit,
) {
    /** The same actions, except that "Eliminar video" calls [ask], so the panel can confirm first. */
    fun confirmingDelete(ask: () -> Unit) = HostVideoActions(video, problem, maxSeconds, onRecord, onImport, ask, onPlayingChange)
}

/**
 * "Video de bienvenida": an optional greeting guests see after tapping the invitation.
 * The host records it with the tablet or picks a file of up to [HostVideoActions.maxSeconds].
 */
@Composable
fun WelcomeSection(
    settings: HostSettings,
    welcome: HostVideoActions,
    onChange: ((HostSettings) -> HostSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PanelCard {
            PanelCardRow(title = stringResource(R.string.host_welcome_show), body = stringResource(R.string.host_welcome_show_body)) {
                PanelSwitch(checked = settings.welcomeVideo, onCheckedChange = { on -> onChange { it.copy(welcomeVideo = on) } })
            }
            CardFooter {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.host_sound), fontSize = 20.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
                    SoundChips(settings.welcomeSound, onPick = { sound -> onChange { it.copy(welcomeSound = sound) } })
                }
            }
        }

        // Dimmed while switched off, but still usable, so the host can prepare it first.
        HostVideoEditor(
            actions = welcome,
            emptyText = stringResource(R.string.host_welcome_none),
            modifier = Modifier.alpha(if (settings.welcomeVideo) 1f else 0.4f),
        )

        IdeaNote(stringResource(welcomeIdea(settings.type)))
    }
}

/** The video's frame on the left; record, pick and delete on the right, with the reason a picked video was refused. */
@Composable
fun HostVideoEditor(actions: HostVideoActions, emptyText: String, modifier: Modifier = Modifier) {
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(actions.onImport) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostVideoPreview(actions.video, emptyText, actions.onPlayingChange)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PillButton(
                text = stringResource(R.string.host_video_record),
                onClick = actions.onRecord,
                icon = R.drawable.ic_video,
                height = 72.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            PillButton(
                text = stringResource(R.string.host_video_pick),
                onClick = { pickVideo.launch(arrayOf("video/*")) },
                style = PillStyle.OUTLINE,
                icon = R.drawable.ic_upload,
                height = 72.dp,
                borderColor = Ivory.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth(),
            )
            if (actions.video != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = actions.onDelete),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_trash), contentDescription = null, tint = Sand, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.host_video_delete), fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (actions.problem != null) {
                Text(
                    text = when (actions.problem) {
                        VideoProblem.TOO_LONG -> stringResource(R.string.host_video_too_long, actions.maxSeconds)
                        VideoProblem.UNREADABLE -> stringResource(R.string.host_video_unreadable)
                    },
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    color = RecordRedLight,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** 300 × 400 frame of the video with its length. A tap plays it right there. */
@Composable
private fun HostVideoPreview(video: HostVideoFile?, emptyText: String, onPlayingChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier.size(width = 300.dp, height = 400.dp).clip(shape).background(SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (video == null) {
            Box(Modifier.fillMaxSize().dashedBorder(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(painterResource(R.drawable.ic_video), contentDescription = null, tint = Sand, modifier = Modifier.size(44.dp))
                    Text(emptyText, fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            HostVideoClip(video, onPlayingChange)
        }
    }
}

/** The video's first frame with a play button, or the video itself once playing. */
@Composable
private fun HostVideoClip(video: HostVideoFile, onPlayingChange: (Boolean) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        var playing by remember(video) { mutableStateOf(false) }
        if (playing) {
            // The host is standing at the tablet, so the preview plays out loud.
            val playback = rememberPlayback(video.file, autoPlay = true, onPlayingChange = onPlayingChange, sound = SoundOutput.SPEAKER)
            LaunchedEffect(playback.ended) { if (playback.ended) playing = false }
            VideoFrame(playback, Modifier.fillMaxSize().clickable { playback.toggle() }, contentScale = ContentScale.Crop)
        } else {
            val frame = rememberVideoFrame(video.file, video.version)
            val seconds by produceState<Int?>(null, video) { value = withContext(Dispatchers.IO) { videoSeconds(video.file) } }
            if (frame != null) Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Scrim)
                    .border(2.dp, Ivory, CircleShape)
                    .clickable(role = Role.Button) { playing = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_play),
                    contentDescription = stringResource(R.string.host_messages_play),
                    tint = Ivory,
                    modifier = Modifier.padding(start = 4.dp).size(30.dp),
                )
            }
            seconds?.let {
                Text(
                    text = formatDuration(it),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ivory,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .background(Scrim, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** "Idea · Los novios dando la bienvenida…", a suggestion that fits the event type. */
@Composable
fun IdeaNote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().dashedBorder(radius = 18.dp).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.host_idea),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = text, fontSize = 20.sp, lineHeight = 29.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
    }
}

@StringRes
private fun welcomeIdea(type: EventType): Int = when (type) {
    EventType.WEDDING -> R.string.host_welcome_idea_wedding
    EventType.CHRISTMAS -> R.string.host_welcome_idea_christmas
    EventType.BIRTHDAY -> R.string.host_welcome_idea_birthday
    EventType.COMPANY -> R.string.host_welcome_idea_company
    EventType.OTHER -> R.string.host_welcome_idea_other
}

private fun Modifier.dashedBorder(radius: Dp = 20.dp): Modifier = drawBehind {
    drawRoundRect(
        color = Ivory.copy(alpha = 0.2f),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}
