package com.olinix.audioguest.ui.host

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.ui.components.ConfirmDialog
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SurfaceDeep
import com.olinix.audioguest.ui.theme.SurfaceRaised

/**
 * Host panel. For now it only lets the host leave the kiosk; the event, welcome video and
 * guest settings from the mockup will be added as sections below the header.
 */
@Composable
fun HostPanelScreen(
    event: EventInfo,
    onClose: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingExit by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Charcoal)) {
        Column(Modifier.fillMaxSize()) {
            Header(event = event, onClose = onClose)
            Column(Modifier.padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 40.dp)) {
                Text(
                    text = stringResource(R.string.host_kiosk_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 52.sp,
                    color = Ivory,
                )
                Text(
                    text = stringResource(R.string.host_kiosk_body),
                    fontSize = 22.sp,
                    color = Sand,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
                ExitCard(onExit = { confirmingExit = true }, modifier = Modifier.padding(top = 36.dp))
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

/** "📞 Panel del anfitrión · Ana & Miguel" and the "Cerrar y volver" button. */
@Composable
private fun Header(event: EventInfo, onClose: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().background(SurfaceDeep)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            PillButton(text = stringResource(R.string.host_close), onClick = onClose, icon = R.drawable.ic_lock, height = 72.dp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ivory.copy(alpha = 0.08f)))
    }
}

@Composable
private fun ExitCard(onExit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceRaised)
            .padding(horizontal = 30.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.host_exit_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ivory,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.host_exit_body),
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
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

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun HostPanelPreview() {
    AudioGuestTheme { HostPanelScreen(EventInfo.Sample, onClose = {}, onExitApp = {}) }
}
