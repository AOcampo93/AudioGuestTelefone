package com.olinix.audioguest.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.rememberVideoFrame
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.GalleryFilter
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.ui.components.AccentCircle
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.CameraBackdrop
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.Waveform
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.menu.EventTitle
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Scrim
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.util.formatClockTime
import com.olinix.audioguest.util.formatDuration
import java.io.File

/** Three rows of tiles fit on screen; a fourth peeks in to show the list scrolls. */
private val TILE_HEIGHT = 256.dp

/**
 * Screen 10 · "Mensajes del evento": saved messages in a two-column grid, filtered by tab.
 * Without [showTabs], as when the host allows only one kind of message, every message is shown.
 */
@Composable
fun GalleryScreen(
    event: EventInfo,
    recordings: List<Recording>,
    filter: GalleryFilter,
    fileOf: (Recording) -> File,
    onFilter: (GalleryFilter) -> Unit,
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
    onLeaveMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showTabs: Boolean = true,
) {
    val shown = if (showTabs) filter.pick(recordings) else recordings

    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .padding(start = 56.dp, end = 56.dp, top = 48.dp),
    ) {
        BackPill(onClick = onBack)
        EventTitle(
            event = event,
            title = stringResource(R.string.gallery_title),
            trailing = pluralStringResource(R.plurals.gallery_count, recordings.size, recordings.size),
            modifier = Modifier.padding(top = 44.dp),
        )
        if (showTabs) {
            FilterTabs(
                selected = filter,
                counts = GalleryFilter.entries.associateWith { it.pick(recordings).size },
                onSelect = onFilter,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
        if (shown.isEmpty()) {
            EmptyGallery(onLeaveMessage = onLeaveMessage, modifier = Modifier.weight(1f).padding(bottom = 64.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                // Tiles scroll all the way to the bottom edge instead of stopping above a margin.
                contentPadding = PaddingValues(top = 28.dp, bottom = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(shown, key = { it.id }) { recording ->
                    val tile = Modifier.fillMaxWidth().height(TILE_HEIGHT)
                    when (recording.mode) {
                        RecordingMode.AUDIO -> VoiceTile(recording, onClick = { onOpen(recording.id) }, modifier = tile)
                        RecordingMode.VIDEO -> VideoTile(recording, fileOf(recording), onClick = { onOpen(recording.id) }, modifier = tile)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    selected: GalleryFilter,
    counts: Map<GalleryFilter, Int>,
    onSelect: (GalleryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = mapOf(
        GalleryFilter.ALL to stringResource(R.string.gallery_tab_all),
        GalleryFilter.VOICE to stringResource(R.string.gallery_tab_voice),
        GalleryFilter.VIDEO to stringResource(R.string.gallery_tab_video),
    )
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(Ivory.copy(alpha = 0.04f))
            .border(1.5.dp, Ivory.copy(alpha = 0.1f), shape)
            .padding(8.dp),
    ) {
        for (tab in GalleryFilter.entries) {
            val active = tab == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(tab) },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = labels.getValue(tab),
                    fontSize = 26.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (active) Charcoal else Ivory,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${counts[tab] ?: 0}",
                    fontSize = 20.sp,
                    color = if (active) Charcoal.copy(alpha = 0.7f) else Sand,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Voice message: time saved, its wave and length, and a play button. */
@Composable
private fun VoiceTile(recording: Recording, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(SurfaceRaised)
            .border(1.5.dp, Ivory.copy(alpha = 0.1f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).border(1.5.dp, accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            TileText(formatClockTime(recording.savedAt))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Seeded by id, so the message shows the same wave here and in the player.
            Waveform(
                color = accent,
                seed = recording.id.toInt(),
                barWidth = 6.dp,
                gap = 6.dp,
                modifier = Modifier.fillMaxWidth().height(90.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDuration(recording.durationSeconds),
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ivory,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f),
            )
            AccentCircle(icon = R.drawable.ic_play, size = 60.dp, iconSize = 24.dp)
        }
    }
}

/** Video message: a frame from the video with a play button, time saved and length. */
@Composable
private fun VideoTile(recording: Recording, file: File, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val frame = rememberVideoFrame(file)
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        if (frame != null) {
            Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CameraBackdrop()
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Scrim)),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp)
                .size(88.dp)
                .background(Scrim, CircleShape)
                .border(2.dp, Ivory, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = Ivory, modifier = Modifier.size(34.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_video), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            TileText(formatClockTime(recording.savedAt), modifier = Modifier.weight(1f).padding(start = 10.dp))
            Text(
                text = formatDuration(recording.durationSeconds),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ivory,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier
                    .background(Scrim, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TileText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ivory, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

/** "Todavía no hay mensajes. ¡Sé el primero!" with a shortcut back to the menu. */
@Composable
private fun EmptyGallery(onLeaveMessage: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .drawBehind {
                    drawCircle(
                        color = Ivory.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, tint = IvorySoft, modifier = Modifier.size(60.dp))
        }
        Text(
            text = stringResource(R.string.gallery_empty_title),
            style = MaterialTheme.typography.displaySmall,
            color = Ivory,
            modifier = Modifier.padding(top = 48.dp),
        )
        Text(
            text = stringResource(R.string.gallery_empty_subtitle),
            style = MaterialTheme.typography.displaySmall,
            fontSize = 44.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = accent,
            modifier = Modifier.padding(top = 8.dp),
        )
        PillButton(
            text = stringResource(R.string.gallery_leave_message),
            onClick = onLeaveMessage,
            icon = R.drawable.ic_mic,
            modifier = Modifier.padding(top = 48.dp),
        )
    }
}

private val PreviewRecordings = listOf(
    Recording(id = 1, eventId = 1, mode = RecordingMode.AUDIO, fileName = "", durationSeconds = 75, savedAt = 1_781_466_120_000),
    Recording(id = 2, eventId = 1, mode = RecordingMode.VIDEO, fileName = "", durationSeconds = 52, savedAt = 1_781_465_880_000),
    Recording(id = 3, eventId = 1, mode = RecordingMode.AUDIO, fileName = "", durationSeconds = 123, savedAt = 1_781_464_800_000),
)

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun GalleryPreview() {
    AudioGuestTheme {
        GalleryScreen(
            event = EventInfo.Sample,
            recordings = PreviewRecordings,
            filter = GalleryFilter.ALL,
            fileOf = { File(it.fileName) },
            onFilter = {},
            onOpen = {},
            onBack = {},
            onLeaveMessage = {},
        )
    }
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun EmptyGalleryPreview() {
    AudioGuestTheme {
        GalleryScreen(EventInfo.Sample, emptyList(), GalleryFilter.ALL, { File(it.fileName) }, {}, {}, {}, {})
    }
}
