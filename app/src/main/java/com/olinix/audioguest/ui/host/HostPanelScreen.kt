package com.olinix.audioguest.ui.host

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.HostSection
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.session.WelcomeProblem
import com.olinix.audioguest.session.WelcomeVideo
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.components.ConfirmDialog
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
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

/**
 * Host panel: Evento, Bienvenida and Invitados, as in the mockup. Every change is saved at
 * once. At the bottom, "Salir de la app" lets the host close the kiosk.
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
    welcome: WelcomeActions,
    modifier: Modifier = Modifier,
) {
    var confirmingExit by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Charcoal)) {
        Column(Modifier.fillMaxSize()) {
            Header(event = event, section = state.section, onSection = onSection, onClose = onClose)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // The keyboard pushes the fields up instead of covering them.
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 40.dp),
            ) {
                SectionHeading(state.section)
                when (state.section) {
                    HostSection.EVENT -> EventSection(settings, event, onChange, Modifier.padding(top = 32.dp))
                    HostSection.WELCOME -> WelcomeSection(
                        settings = settings,
                        welcome = welcome.video,
                        problem = welcome.problem,
                        maxSeconds = welcome.maxSeconds,
                        onChange = onChange,
                        onRecord = welcome.onRecord,
                        onImport = welcome.onImport,
                        onDelete = welcome.onDelete,
                        onPlayingChange = welcome.onPlayingChange,
                        modifier = Modifier.padding(top = 40.dp),
                    )
                    HostSection.GUESTS -> GuestsSection(settings, onChange, Modifier.padding(top = 36.dp))
                }
                ExitCard(onExit = { confirmingExit = true }, modifier = Modifier.padding(top = 48.dp))
            }
        }

        if (confirmingExit) {
            ConfirmDialog(
                icon = R.drawable.ic_exit,
                title = stringResource(R.string.host_exit_confirm_title),
                body = stringResource(R.string.host_exit_confirm_body),
                keepText = stringResource(R.string.host_exit_keep),
                confirmText = stringResource(R.string.host_exit_confirm),
                onKeep = { confirmingExit = false },
                onConfirm = onExitApp,
            )
        }
    }
}

/** What the "Bienvenida" section shows and can do, bundled so the panel's signature stays short. */
class WelcomeActions(
    val video: WelcomeVideo?,
    val problem: WelcomeProblem?,
    val maxSeconds: Int,
    val onRecord: () -> Unit,
    val onImport: (Uri) -> Unit,
    val onDelete: () -> Unit,
    val onPlayingChange: (Boolean) -> Unit,
)

/** "📞 Panel del anfitrión · Ana & Miguel", "Cerrar y volver" and the three section tabs. */
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
                    Text(
                        text = event.hosts,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 28.sp,
                        fontStyle = FontStyle.Italic,
                        color = accent,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                PillButton(text = stringResource(R.string.host_close), onClick = onClose, icon = R.drawable.ic_lock, height = 72.dp)
            }
            Tabs(current = section, onSection = onSection)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ivory.copy(alpha = 0.08f)))
    }
}

@Composable
private fun Tabs(current: HostSection, onSection: (HostSection) -> Unit) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier.fillMaxWidth().clip(pill).background(TabTrack).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (section in HostSection.entries) {
            val selected = section == current
            val color = if (selected) MaterialTheme.colorScheme.primary else Taupe
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .clip(pill)
                    .then(if (selected) Modifier.background(TabSelected) else Modifier)
                    .clickable(role = Role.Tab) { onSection(section) },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(section.icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(
                    text = stringResource(section.tabLabel),
                    fontSize = 21.sp,
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
        HostSection.GUESTS -> R.string.host_guests_title to R.string.host_guests_body
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
        HostSection.GUESTS -> R.drawable.ic_guests
    }

private val HostSection.tabLabel: Int
    get() = when (this) {
        HostSection.EVENT -> R.string.host_tab_event
        HostSection.WELCOME -> R.string.host_tab_welcome
        HostSection.GUESTS -> R.string.host_tab_guests
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
            welcome = WelcomeActions(null, null, 30, {}, {}, {}, {}),
        )
    }
}
