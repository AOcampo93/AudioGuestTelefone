package com.olinix.audioguest.ui.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.ExportState
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.components.PillStyle
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.RecordRedLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.SavedGreen

/** "Exportar para o computador" and "Partilhar tudo", for the active event's messages. */
@Composable
fun ExportButtons(working: Boolean, onExport: (share: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton(
            text = stringResource(R.string.host_export_cable),
            onClick = { onExport(false) },
            style = PillStyle.OUTLINE,
            icon = R.drawable.ic_upload,
            height = 72.dp,
            borderColor = Ivory.copy(alpha = 0.3f),
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        )
        PillButton(
            text = stringResource(R.string.host_export_share),
            onClick = { onExport(true) },
            style = PillStyle.OUTLINE,
            icon = R.drawable.ic_share,
            height = 72.dp,
            borderColor = Ivory.copy(alpha = 0.3f),
            enabled = !working,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** How the last export went: copying, where the files are and how to take them by cable, or that it failed. */
@Composable
fun ExportStatusCard(state: ExportState, modifier: Modifier = Modifier) {
    PanelCard(modifier) {
        when (state) {
            is ExportState.Working -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                Text(stringResource(R.string.host_export_working), fontSize = 22.sp, color = Ivory, style = MaterialTheme.typography.bodyMedium)
            }
            is ExportState.Done -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = SavedGreen, modifier = Modifier.size(26.dp))
                    Text(
                        text = pluralStringResource(R.plurals.host_export_done, state.result.total, state.result.total) +
                            " (" + pluralStringResource(R.plurals.host_export_new, state.result.copied, state.result.copied) + ")",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ivory,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(state.result.folder, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.host_export_cable_help), fontSize = 20.sp, lineHeight = 28.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
            }
            is ExportState.Failed -> Text(stringResource(R.string.host_export_failed), fontSize = 22.sp, color = RecordRedLight, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
