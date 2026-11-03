package com.olinix.audioguest.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.VideoFrame
import com.olinix.audioguest.audio.rememberPlayback
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.WelcomeVideo
import com.olinix.audioguest.ui.components.ProgressBar
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.show.OverlayChip
import com.olinix.audioguest.ui.show.VideoShade
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Surface

/**
 * Screen 13 · The host's welcome video, heard through the handset. "Saltar", a tap anywhere
 * or the end of the video moves on to the menu.
 */
@Composable
fun WelcomeScreen(
    welcome: WelcomeVideo,
    event: EventInfo,
    onFinish: () -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val playback = rememberPlayback(welcome.file, autoPlay = true, onPlayingChange = onPlayingChange)
    LaunchedEffect(playback.ended) { if (playback.ended) onFinish() }

    Box(
        modifier
            .fillMaxSize()
            .screenBackground()
            .clickable(interactionSource = null, indication = null, onClick = onFinish),
    ) {
        VideoFrame(playback, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(VideoShade))

        Column(Modifier.padding(56.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OverlayChip(R.drawable.ic_video, stringResource(R.string.welcome_chip), iconSize = 28.dp, bold = true)
            OverlayChip(R.drawable.ic_phone, stringResource(R.string.player_listen_handset), outlined = true)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 56.dp, end = 56.dp, bottom = 76.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Column {
                Text(text = event.label, style = MaterialTheme.typography.headlineMedium, fontSize = 40.sp, fontStyle = FontStyle.Italic, color = IvorySoft)
                Text(
                    text = event.hosts,
                    style = MaterialTheme.typography.displayLarge.copy(lineBreak = LineBreak.Heading),
                    fontSize = 84.sp,
                    lineHeight = 88.sp,
                    color = Ivory,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SkipButton(onClick = onFinish, modifier = Modifier.align(Alignment.End))
        }

        ProgressBar(
            progress = playback.progress,
            color = accent,
            height = 6.dp,
            track = Ivory.copy(alpha = 0.22f),
            glide = false,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 56.dp, end = 56.dp, bottom = 40.dp),
        )
    }
}

/** "Saltar ⏭" */
@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(96.dp)
            .clip(shape)
            .background(Surface.copy(alpha = 0.72f))
            .border(2.dp, Ivory.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(start = 44.dp, end = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.welcome_skip), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ivory, style = MaterialTheme.typography.titleLarge)
        Icon(painterResource(R.drawable.ic_next), contentDescription = null, tint = Ivory, modifier = Modifier.size(30.dp))
    }
}
