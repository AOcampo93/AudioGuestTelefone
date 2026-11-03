package com.olinix.audioguest.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.data.EventSummary
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.HostSection
import com.olinix.audioguest.session.HostVideo
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.components.ConfirmDialog
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.eventInfoOf
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SavedGreen
import com.olinix.audioguest.ui.theme.SurfaceDeep
import com.olinix.audioguest.ui.theme.TabSelected
import com.olinix.audioguest.ui.theme.TabTrack
import com.olinix.audioguest.ui.theme.Taupe
import java.io.File

/**
 * Host panel: Evento, Bienvenida, Llamada, Invitados and Mensajes. Every change is saved at once.
 * At the bottom, "Salir de la app" lets the host close the kiosk.
 */
@Composable
fun HostPanelScreen(
    state: SessionState.HostPanel,
    settings: HostSettings,
    event: EventInfo,
    onSection: (HostSection) -> Unit,
    onChange: ((HostSettings) -> HostSettings) -> Unit,
    onClose: () -> Unit,
    onExitApp: () -> Unit,
    welcome: HostVideoActions,
    call: HostVideoActions,
    onTestCall: () -> Unit,
    messages: MessageActions,
    events: EventActions,
    modifier: Modifier = Modifier,
) {
    // Drawn above the whole panel rather than inside the scrolling section that opened it.
    var overlay by remember { mutableStateOf<PanelOverlay?>(null) }
    val padding = PaddingValues(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 40.dp)
    val exitCard = @Composable { ExitCard(onExit = { overlay = PanelOverlay.Exit }, modifier = Modifier.padding(top = 48.dp)) }

    Box(modifier.fillMaxSize().background(Charcoal)) {
        Column(Modifier.fillMaxSize()) {
            Header(event = event, section = state.section, onSection = onSection, onClose = onClose)
            if (state.section == HostSection.MESSAGES) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { SectionHeading(state.section) }
                    messageItems(
                        messages = messages,
                        settings = settings,
                        onPlay = { overlay = PanelOverlay.PlayMessage(it) },
                        onDelete = { overlay = PanelOverlay.DeleteMessage(it) },
                    )
                    item { exitCard() }
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // The keyboard pushes the fields up instead of covering them.
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(padding),
                ) {
                    SectionHeading(state.section)
                    when (state.section) {
                        HostSection.EVENT -> EventSection(settings, event, onChange, Modifier.padding(top = 32.dp))
                        HostSection.WELCOME -> WelcomeSection(
                            settings = settings,
                            welcome = welcome.confirmingDelete { overlay = PanelOverlay.DeleteHostVideo(HostVideo.WELCOME) },
                            onChange = onChange,
                            modifier = Modifier.padding(top = 40.dp),
                        )
                        HostSection.CALL -> CallSection(
                            settings = settings,
                            question = call.confirmingDelete { overlay = PanelOverlay.DeleteHostVideo(HostVideo.CALL) },
                            onChange = onChange,
                            onTestCall = onTestCall,
                            modifier = Modifier.padding(top = 40.dp),
                        )
                        HostSection.GUESTS -> GuestsSection(settings, onChange, Modifier.padding(top = 36.dp))
                        HostSection.EVENTS -> EventsSection(
                            actions = events,
                            onAskDelete = { overlay = PanelOverlay.DeleteEvent(it) },
                            modifier = Modifier.padding(top = 36.dp),
                        )
                        HostSection.MESSAGES -> Unit
                    }
                    exitCard()
                }
            }
        }

        when (val shown = overlay) {
            null -> Unit
            PanelOverlay.Exit -> ConfirmDialog(
                icon = R.drawable.ic_exit,
                title = stringResource(R.string.host_exit_confirm_title),
                body = stringResource(R.string.host_exit_confirm_body),
                keepText = stringResource(R.string.host_exit_keep),
                confirmText = stringResource(R.string.host_exit_confirm),
                onKeep = { overlay = null },
                onConfirm = onExitApp,
            )
            is PanelOverlay.DeleteHostVideo -> ConfirmDialog(
                icon = R.drawable.ic_trash,
                title = stringResource(if (shown.kind == HostVideo.WELCOME) R.string.host_welcome_delete_title else R.string.host_call_delete_title),
                body = stringResource(if (shown.kind == HostVideo.WELCOME) R.string.host_welcome_delete_body else R.string.host_call_delete_body),
                keepText = stringResource(R.string.host_delete_keep),
                confirmText = stringResource(R.string.host_delete_confirm),
                onKeep = { overlay = null },
                onConfirm = {
                    overlay = null
                    (if (shown.kind == HostVideo.WELCOME) welcome else call).onDelete()
                },
            )
            is PanelOverlay.DeleteMessage -> ConfirmDialog(
                icon = R.drawable.ic_trash,
                title = stringResource(R.string.host_messages_delete_title),
                body = stringResource(R.string.host_messages_delete_body),
                keepText = stringResource(R.string.host_delete_keep),
                confirmText = stringResource(R.string.host_delete_confirm),
                onKeep = { overlay = null },
                onConfirm = {
                    overlay = null
                    messages.onDelete(shown.recording.id)
                },
            )
            is PanelOverlay.DeleteEvent -> ConfirmDialog(
                icon = R.drawable.ic_trash,
                title = stringResource(R.string.host_events_delete_title, eventInfoOf(shown.summary.event.settings).headline),
                body = pluralStringResource(R.plurals.host_events_delete_body, shown.summary.messageCount, shown.summary.messageCount),
                keepText = stringResource(R.string.host_delete_keep),
                confirmText = stringResource(R.string.host_delete_confirm),
                onKeep = { overlay = null },
                onConfirm = {
                    overlay = null
                    events.onDelete(shown.summary.event.id)
                },
            )
            is PanelOverlay.PlayMessage -> MessagePlayerOverlay(
                recording = shown.recording,
                file = messages.fileOf(shown.recording),
                onClose = { overlay = null },
                onPlayingChange = welcome.onPlayingChange,
            )
        }
    }
}

/** What can cover the panel: a confirmation, or a message playing. */
private sealed interface PanelOverlay {
    data object Exit : PanelOverlay
    data class DeleteHostVideo(val kind: HostVideo) : PanelOverlay
    data class DeleteMessage(val recording: Recording) : PanelOverlay
    data class DeleteEvent(val summary: EventSummary) : PanelOverlay
    data class PlayMessage(val recording: Recording) : PanelOverlay
}

/** "📞 Panel del anfitrión · Ana & Miguel", "Cerrar y volver" and the section tabs. */
@Composable
private fun Header(event: EventInfo, section: HostSection, onSection: (HostSection) -> Unit, onClose: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().background(SurfaceDeep)) {
        Column(
            modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                        Text(
                            text = stringResource(R.string.host_panel_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ivory,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    // The active event, with the way to the list of events.
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onSection(HostSection.EVENTS) },
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = event.hosts,
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 28.sp,
                            fontStyle = FontStyle.Italic,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Row(
                            modifier = Modifier
                                .border(1.5.dp, Ivory.copy(alpha = 0.3f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = Ivory, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.host_events_switch),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ivory,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                PillButton(text = stringResource(R.string.host_close), onClick = onClose, icon = R.drawable.ic_lock, height = 72.dp)
            }
            Tabs(current = section, onSection = onSection)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ivory.copy(alpha = 0.08f)))
    }
}

/** Five tabs, each with its icon above the name so they fit side by side. */
@Composable
private fun Tabs(current: HostSection, onSection: (HostSection) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(TabTrack).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (section in HostSection.entries.filter { it != HostSection.EVENTS }) {
            val selected = section == current
            val color = if (selected) MaterialTheme.colorScheme.primary else Taupe
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(if (selected) Modifier.background(TabSelected) else Modifier)
                    .clickable(role = Role.Tab) { onSection(section) },
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(section.icon), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                Text(
                    text = stringResource(section.tabLabel),
                    fontSize = 19.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** "Evento · Tipo de evento, nombres, fecha y color." with "✓ Guardado automático" on the right. */
@Composable
private fun SectionHeading(section: HostSection) {
    val (title, body) = when (section) {
        HostSection.EVENT -> R.string.host_event_title to R.string.host_event_body
        HostSection.WELCOME -> R.string.host_welcome_title to R.string.host_welcome_body
        HostSection.CALL -> R.string.host_call_title to R.string.host_call_body
        HostSection.GUESTS -> R.string.host_guests_title to R.string.host_guests_body
        HostSection.MESSAGES -> R.string.host_messages_title to R.string.host_messages_body
        HostSection.EVENTS -> R.string.host_events_title to R.string.host_events_body
    }
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.displaySmall, fontSize = 52.sp, lineHeight = 54.sp, color = Ivory)
            Text(
                text = stringResource(body),
                fontSize = 22.sp,
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = SavedGreen, modifier = Modifier.size(22.dp))
            Text(text = stringResource(R.string.host_autosaved), fontSize = 20.sp, color = SavedGreen, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExitCard(onExit: () -> Unit, modifier: Modifier = Modifier) {
    PanelCard(modifier) {
        PanelCardRow(title = stringResource(R.string.host_exit_title), body = stringResource(R.string.host_exit_body)) {
            PillButton(
                text = stringResource(R.string.host_exit),
                onClick = onExit,
                style = PillStyle.OUTLINE,
                icon = R.drawable.ic_exit,
                height = 72.dp,
                contentColor = RecordRedLight,
                borderColor = RecordRedLight.copy(alpha = 0.6f),
            )
        }
    }
}

private val HostSection.icon: Int
    get() = when (this) {
        HostSection.EVENT -> R.drawable.ic_calendar
        HostSection.WELCOME -> R.drawable.ic_video
        HostSection.CALL -> R.drawable.ic_phone
        HostSection.GUESTS -> R.drawable.ic_guests
        HostSection.MESSAGES -> R.drawable.ic_gallery
        HostSection.EVENTS -> R.drawable.ic_calendar
    }

private val HostSection.tabLabel: Int
    get() = when (this) {
        HostSection.EVENT -> R.string.host_tab_event
        HostSection.WELCOME -> R.string.host_tab_welcome
        HostSection.CALL -> R.string.host_tab_call
        HostSection.GUESTS -> R.string.host_tab_guests
        HostSection.MESSAGES -> R.string.host_tab_messages
        HostSection.EVENTS -> R.string.host_events_title
    }

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun HostPanelPreview() {
    AudioGuestTheme {
        HostPanelScreen(
            state = SessionState.HostPanel(),
            settings = HostSettings(),
            event = EventInfo.Sample,
            onSection = {},
            onChange = {},
            onClose = {},
            onExitApp = {},
            welcome = HostVideoActions(null, null, 30, {}, {}, {}, {}),
            call = HostVideoActions(null, null, 30, {}, {}, {}, {}),
            onTestCall = {},
            messages = MessageActions(emptyList(), { File("") }, {}),
            events = EventActions(emptyList(), null, {}, {}, {}),
        )
    }
}
