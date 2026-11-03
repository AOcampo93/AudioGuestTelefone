package com.olinix.audioguest.ui.host

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.components.PillButton
import com.olinix.audioguest.ui.theme.AmberLight
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Taupe

/**
 * "Modo llamada": now and then the booth rings; whoever answers sees the host's question and
 * records their answer. The switch needs the question video first.
 */
@Composable
fun CallSection(
    settings: HostSettings,
    question: HostVideoActions,
    onChange: ((HostSettings) -> HostSettings) -> Unit,
    onTestCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasQuestion = question.video != null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PanelCard {
            PanelCardRow(title = stringResource(R.string.host_call_switch), body = stringResource(R.string.host_call_switch_body)) {
                PanelSwitch(
                    checked = settings.callMode && hasQuestion,
                    onCheckedChange = { on -> onChange { it.copy(callMode = on) } },
                    enabled = hasQuestion,
                )
            }
            CardFooter {
                if (!hasQuestion) {
                    Text(stringResource(R.string.host_call_needs_video), fontSize = 20.sp, color = AmberLight, style = MaterialTheme.typography.bodyMedium)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.host_call_every), fontSize = 20.sp, color = Taupe, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (minutes in HostSettings.CALL_INTERVALS) {
                                PanelChip(
                                    text = stringResource(R.string.host_minutes, minutes),
                                    selected = minutes == settings.callEveryMinutes,
                                    onClick = { onChange { it.copy(callEveryMinutes = minutes) } },
                                    height = 56,
                                    fontSize = 20,
                                )
                            }
                        }
                        Text(stringResource(R.string.host_call_every_note), fontSize = 20.sp, color = Sand, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelLabel(stringResource(R.string.host_call_video))
            HostVideoEditor(actions = question, emptyText = stringResource(R.string.host_call_none))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelLabel(stringResource(R.string.host_call_answer))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Only the kinds guests are allowed to leave.
                for (mode in RecordingMode.entries.filter(settings::allows)) {
                    PanelChip(
                        text = stringResource(if (mode == RecordingMode.AUDIO) R.string.host_call_answer_voice else R.string.host_call_answer_video),
                        selected = mode == settings.callAnswerMode,
                        onClick = { onChange { it.copy(callAnswer = mode) } },
                    )
                }
            }
        }

        PanelCard {
            PanelCardRow(title = stringResource(R.string.host_call_test), body = stringResource(R.string.host_call_test_body)) {
                PillButton(text = stringResource(R.string.host_call_test_button), onClick = onTestCall, icon = R.drawable.ic_phone, height = 72.dp)
            }
        }

        IdeaNote(stringResource(callIdea(settings.type)))
    }
}

@StringRes
private fun callIdea(type: EventType): Int = when (type) {
    EventType.WEDDING -> R.string.host_call_idea_wedding
    EventType.CHRISTMAS -> R.string.host_call_idea_christmas
    EventType.BIRTHDAY -> R.string.host_call_idea_birthday
    EventType.COMPANY -> R.string.host_call_idea_company
    EventType.OTHER -> R.string.host_call_idea_other
}
