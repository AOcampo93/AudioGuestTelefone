package com.olinix.audioguest.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.Ivory

/** One option of a [ChoiceToggle]. */
class ToggleChoice<T>(val value: T, @param:DrawableRes val icon: Int, val label: String)

/** Side-by-side options in a pill, such as "Auricular | Tablet". The chosen one is filled with the accent. */
@Composable
fun <T> ChoiceToggle(choices: List<ToggleChoice<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(pill)
            .border(1.5.dp, Ivory.copy(alpha = 0.22f), pill)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (choice in choices) {
            val active = choice.value == selected
            val color = if (active) Charcoal else Ivory
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(pill)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(choice.value) }
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(choice.icon), contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                Text(
                    text = choice.label,
                    fontSize = 22.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** "📞 Auricular | ▭ Tablet": the microphone a guest records with. */
@Composable
fun MicrophoneToggle(selected: Microphone, onSelect: (Microphone) -> Unit, modifier: Modifier = Modifier) {
    ChoiceToggle(
        choices = listOf(
            ToggleChoice(Microphone.HANDSET, R.drawable.ic_phone, stringResource(R.string.route_handset)),
            ToggleChoice(Microphone.TABLET, R.drawable.ic_tablet, stringResource(R.string.route_tablet)),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** "📞 Auricular | 🔊 Altavoz": where a guest hears a message. */
@Composable
fun SoundToggle(selected: SoundOutput, onSelect: (SoundOutput) -> Unit, modifier: Modifier = Modifier) {
    ChoiceToggle(
        choices = listOf(
            ToggleChoice(SoundOutput.HANDSET, R.drawable.ic_phone, stringResource(R.string.route_handset)),
            ToggleChoice(SoundOutput.SPEAKER, R.drawable.ic_speaker, stringResource(R.string.route_speaker)),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}
