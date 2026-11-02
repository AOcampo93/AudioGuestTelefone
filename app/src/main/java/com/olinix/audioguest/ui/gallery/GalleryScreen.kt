package com.olinix.audioguest.ui.gallery

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.ui.components.BackPill
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.screenBackground
import com.olinix.audioguest.ui.menu.EventTitle
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand

/** Which messages the gallery shows. */
enum class GalleryFilter { ALL, VOICE, VIDEO }

/**
 * Screen 10 · "Mensajes del evento". Saved recordings arrive in phase 3;
 * until then the gallery shows its empty state.
 */
@Composable
fun GalleryScreen(
    event: EventInfo,
    onBack: () -> Unit,
    onLeaveMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(GalleryFilter.ALL) }
    val total = 0

    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .padding(start = 56.dp, end = 56.dp, top = 48.dp, bottom = 64.dp),
    ) {
        BackPill(onClick = onBack)
        EventTitle(
            event = event,
            title = stringResource(R.string.gallery_title),
            trailing = pluralStringResource(R.plurals.gallery_count, total, total),
            modifier = Modifier.padding(top = 44.dp),
        )
        FilterTabs(
            selected = filter,
            counts = mapOf(GalleryFilter.ALL to 0, GalleryFilter.VOICE to 0, GalleryFilter.VIDEO to 0),
            onSelect = { filter = it },
            modifier = Modifier.padding(top = 28.dp),
        )
        EmptyGallery(onLeaveMessage = onLeaveMessage, modifier = Modifier.weight(1f))
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

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun GalleryPreview() {
    AudioGuestTheme { GalleryScreen(EventInfo.Sample, onBack = {}, onLeaveMessage = {}) }
}

