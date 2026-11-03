package com.olinix.audioguest.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.data.EventSummary
import com.olinix.audioguest.session.ExportState
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.eventInfoOf
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceRaised

/** What the "Eventos" list shows and can do, bundled so the panel's signature stays short. */
class EventActions(
    /** Every event, newest first. */
    val events: List<EventSummary>,
    val activeId: Long?,
    val onUse: (Long) -> Unit,
    val onCreate: () -> Unit,
    /** Deletes an event. The panel asks the host to confirm first. */
    val onDelete: (Long) -> Unit,
    val exportState: ExportState? = null,
    /** Copies an event's messages to the Download folder, for the cable. */
    val onExport: (Long) -> Unit = {},
)

/**
 * "Eventos": every event the booth has worked at. Each keeps its own setup and messages, so the
 * booth only shows and records into the active one. Past events can be reopened or deleted.
 */
@Composable
fun EventsSection(actions: EventActions, onAskDelete: (EventSummary) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PillButton(
            text = stringResource(R.string.host_events_new),
            onClick = actions.onCreate,
            icon = R.drawable.ic_calendar,
            height = 72.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        actions.exportState?.let { ExportStatusCard(it) }
        for (summary in actions.events) {
            EventRow(
                summary = summary,
                active = summary.event.id == actions.activeId,
                exporting = actions.exportState is ExportState.Working,
                onUse = { actions.onUse(summary.event.id) },
                onExport = { actions.onExport(summary.event.id) },
                onDelete = { onAskDelete(summary) },
            )
        }
    }
}

/** "Boda de Ana & Miguel · 14 de junio de 2027 · 12 mensajes", its export button, then "Activo", or "Usar" and the bin. */
@Composable
private fun EventRow(
    summary: EventSummary,
    active: Boolean,
    exporting: Boolean,
    onUse: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val info = eventInfoOf(summary.event.settings)
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceRaised)
            .then(if (active) Modifier.border(2.dp, accent, shape) else Modifier)
            .padding(horizontal = 26.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(info.headline, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ivory, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = info.date + " · " + pluralStringResource(R.plurals.host_messages_total, summary.messageCount, summary.messageCount),
                fontSize = 20.sp,
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (summary.messageCount > 0) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Ivory.copy(alpha = 0.3f), CircleShape)
                    .clickable(enabled = !exporting, onClick = onExport),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_upload),
                    contentDescription = stringResource(R.string.host_events_export),
                    tint = Ivory,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (active) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.host_events_active), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            PillButton(
                text = stringResource(R.string.host_events_use),
                onClick = onUse,
                style = PillStyle.OUTLINE,
                height = 64.dp,
                borderColor = Ivory.copy(alpha = 0.3f),
            )
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, RecordRedLight.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(R.string.host_events_delete),
                    tint = RecordRedLight,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
