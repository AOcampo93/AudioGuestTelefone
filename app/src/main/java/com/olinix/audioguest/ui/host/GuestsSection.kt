package com.olinix.audioguest.ui.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.settings.AllowedMessages
import com.olinix.audioguest.settings.GuestLanguage
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.theme.AmberLight
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.IvorySoft
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Surface
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe

/** "Experiencia del invitado": screen language, whether guests see the messages, and show mode. */
@Composable
fun GuestsSection(
    settings: HostSettings,
    onChange: ((HostSettings) -> HostSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            PanelLabel(stringResource(R.string.host_language))
            LanguagePicker(settings.language, onPick = { language -> onChange { it.copy(language = language) } }, Modifier.padding(top = 12.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelLabel(stringResource(R.string.host_allowed))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (allowed in AllowedMessages.entries) {
                    PanelChip(
                        text = allowedLabel(allowed),
                        selected = allowed == settings.allowed,
                        onClick = { onChange { it.copy(allowed = allowed) } },
                    )
                }
            }
            Text(stringResource(R.string.host_allowed_body), fontSize = 20.sp, lineHeight = 28.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
        }

        PanelCard {
            PanelCardRow(
                title = stringResource(R.string.host_show_messages),
                body = stringResource(R.string.host_show_messages_body),
            ) {
                PanelSwitch(checked = settings.showMessages, onCheckedChange = { on -> onChange { it.copy(showMessages = on) } })
            }
        }

        PanelCard {
            PanelCardRow(
                title = stringResource(R.string.host_show_mode),
                body = stringResource(R.string.host_show_mode_body),
            ) {
                PanelSwitch(
                    checked = settings.showModeActive,
                    onCheckedChange = { on -> onChange { it.copy(showMode = on) } },
                    enabled = settings.showMessages,
                )
            }
            when {
                !settings.showMessages -> CardFooter {
                    Text(
                        text = stringResource(R.string.host_show_needs_messages),
                        fontSize = 20.sp,
                        color = AmberLight,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                settings.showModeActive -> CardFooter {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.host_show_after), fontSize = 20.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (seconds in HostSettings.SHOW_DELAYS) {
                                PanelChip(
                                    text = delayLabel(seconds),
                                    selected = seconds == settings.showAfterSeconds,
                                    onClick = { onChange { it.copy(showAfterSeconds = seconds) } },
                                    height = 56,
                                    fontSize = 20,
                                )
                            }
                        }
                        Text(stringResource(R.string.host_show_idle), fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Português · Español · English, with Portuguese marked as the default. */
@Composable
private fun LanguagePicker(current: GuestLanguage, onPick: (GuestLanguage) -> Unit, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier.fillMaxWidth().clip(pill).background(SurfaceRaised).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (language in GuestLanguage.entries) {
            val selected = language == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(pill)
                    .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
                    .clickable(role = Role.RadioButton) { onPick(language) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = languageName(language),
                    fontSize = 24.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Surface else IvorySoft,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (language == HostSettings().language) {
                    Text(
                        text = stringResource(R.string.host_language_default),
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Surface else Sand,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = if (selected) Modifier.alpha(0.7f) else Modifier,
                    )
                }
            }
        }
    }
}

/** Each language is named in itself, so a guest who reads only that one still finds it. */
@Composable
private fun languageName(language: GuestLanguage): String = stringResource(
    when (language) {
        GuestLanguage.PORTUGUESE -> R.string.host_language_pt
        GuestLanguage.SPANISH -> R.string.host_language_es
        GuestLanguage.ENGLISH -> R.string.host_language_en
    }
)

@Composable
private fun allowedLabel(allowed: AllowedMessages): String = stringResource(
    when (allowed) {
        AllowedMessages.BOTH -> R.string.host_allowed_both
        AllowedMessages.VOICE_ONLY -> R.string.host_allowed_voice
        AllowedMessages.VIDEO_ONLY -> R.string.host_allowed_video
    }
)

@Composable
private fun delayLabel(seconds: Int): String =
    if (seconds < 60) stringResource(R.string.host_seconds, seconds) else stringResource(R.string.host_minutes, seconds / 60)
