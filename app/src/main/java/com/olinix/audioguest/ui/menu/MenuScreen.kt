package com.olinix.audioguest.ui.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe

/** Screen 2 · Menu: two big cards for voice and video, a quieter one for the gallery. */
@Composable
fun MenuScreen(
    event: EventInfo,
    config: SessionConfig,
    onBack: () -> Unit,
    onChoose: (RecordingMode) -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
    /** Off when the host hides the messages from guests. */
    showGallery: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .padding(start = 56.dp, end = 56.dp, top = 48.dp, bottom = 64.dp),
    ) {
        BackPill(onClick = onBack)
        EventTitle(event = event, title = stringResource(R.string.menu_title), modifier = Modifier.padding(top = 44.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ModeCard(
                icon = R.drawable.ic_mic,
                limit = stringResource(R.string.menu_up_to_minutes, config.maxAudioSeconds / 60),
                title = stringResource(R.string.voice_message),
                hintIcon = R.drawable.ic_phone,
                hint = stringResource(R.string.speak_into_handset),
                onClick = { onChoose(RecordingMode.AUDIO) },
            )
            ModeCard(
                icon = R.drawable.ic_video,
                limit = stringResource(R.string.menu_up_to_minutes, config.maxVideoSeconds / 60),
                title = stringResource(R.string.video_message),
                hintIcon = R.drawable.ic_arrow_right,
                hint = stringResource(R.string.menu_video_hint),
                onClick = { onChoose(RecordingMode.VIDEO) },
            )
            if (showGallery) GalleryCard(onClick = onGallery)
        }
    }
}

/** Italic "Boda de Ana & Miguel" above a big serif title. Shared with the gallery. */
@Composable
fun EventTitle(event: EventInfo, title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Column(modifier) {
        Text(
            text = event.headline,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                fontSize = 68.sp,
                lineHeight = 72.sp,
                color = Ivory,
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sand,
                    modifier = Modifier.padding(start = 18.dp, bottom = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ModeCard(
    @DrawableRes icon: Int,
    limit: String,
    title: String,
    @DrawableRes hintIcon: Int,
    hint: String,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceRaised)
            .border(2.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 44.dp, vertical = 36.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AccentCircle(icon = icon, size = 132.dp, iconSize = 60.dp)
            Text(text = limit, fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontSize = 56.sp,
            lineHeight = 58.sp,
            color = Ivory,
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(hintIcon), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Text(text = hint, style = MaterialTheme.typography.bodyLarge, color = Taupe)
        }
    }
}

@Composable
private fun GalleryCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(shape)
            .border(1.5.dp, Ivory.copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .border(1.5.dp, Ivory.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, tint = IvorySoft, modifier = Modifier.size(42.dp))
        }
        Column {
            Text(
                text = stringResource(R.string.menu_gallery),
                style = MaterialTheme.typography.displaySmall,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                fontStyle = FontStyle.Normal,
                color = Ivory,
            )
            Text(
                text = stringResource(R.string.menu_gallery_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Sand,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun MenuScreenPreview() {
    AudioGuestTheme {
        MenuScreen(EventInfo.Sample, SessionConfig(), onBack = {}, onChoose = {}, onGallery = {})
    }
}
