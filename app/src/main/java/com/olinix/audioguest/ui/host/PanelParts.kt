package com.olinix.audioguest.ui.host

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.ui.theme.FieldBackground
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Muted
import com.olinix.audioguest.ui.theme.Sand
import com.olinix.audioguest.ui.theme.Surface
import com.olinix.audioguest.ui.theme.SurfaceRaised
import com.olinix.audioguest.ui.theme.Taupe

private val FieldShape = RoundedCornerShape(16.dp)

/** Small bold caption above a field or a group: "Tipo de evento". */
@Composable
fun PanelLabel(text: String, modifier: Modifier = Modifier, note: String? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Taupe, style = MaterialTheme.typography.bodyMedium)
        if (note != null) Text(text = note, fontSize = 20.sp, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * One-line text field. It keeps its own copy of the text, so typing never waits for the
 * saved settings to come back, and it cuts the text at [maxLength] as the mockup asks.
 */
@Composable
fun PanelField(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
) {
    var text by remember { mutableStateOf(value) }
    BasicTextField(
        value = text,
        onValueChange = { typed ->
            val cleaned = (if (numeric) typed.filter(Char::isDigit) else typed).take(maxLength)
            text = cleaned
            onValueChange(cleaned)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 26.sp, color = Ivory),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = if (numeric) KeyboardCapitalization.None else KeyboardCapitalization.Words,
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier.height(72.dp),
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(FieldShape)
                    .background(FieldBackground)
                    .border(1.5.dp, Ivory.copy(alpha = 0.2f), FieldShape)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) Text(text = placeholder, fontSize = 26.sp, color = Muted, style = MaterialTheme.typography.bodyLarge)
                field()
            }
        },
    )
}

/** Looks like [PanelField] but opens something on tap, such as the date picker. */
@Composable
fun PanelButtonField(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(FieldShape)
            .background(FieldBackground)
            .border(1.5.dp, Ivory.copy(alpha = 0.2f), FieldShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, fontSize = 26.sp, color = Ivory, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Rounded choice such as an event type or a show delay. The chosen one is filled with the accent. */
@Composable
fun PanelChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, height: Int = 64, fontSize: Int = 22) {
    val shape = RoundedCornerShape(percent = 50)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(accent) else Modifier.border(1.5.dp, Ivory.copy(alpha = 0.24f), shape))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = (if (height >= 64) 28 else 24).dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Surface else Ivory,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** On/off switch in the mockup's style: 96 × 52 with a sliding ivory knob. */
@Composable
fun PanelSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val track by animateColorAsState(
        when {
            !enabled -> Ivory.copy(alpha = 0.08f)
            checked -> MaterialTheme.colorScheme.primary
            else -> Ivory.copy(alpha = 0.18f)
        },
        label = "switchTrack",
    )
    val knobX by animateDpAsState(if (checked && enabled) 49.dp else 5.dp, label = "switchKnob")
    Box(
        modifier = modifier
            .size(width = 96.dp, height = 52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(track)
            .then(if (enabled) Modifier.clickable(role = Role.Switch) { onCheckedChange(!checked) } else Modifier),
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobX.roundToPx(), 5.dp.roundToPx()) }
                .size(42.dp)
                .background(if (enabled) Ivory else Ivory.copy(alpha = 0.25f), CircleShape)
        )
    }
}

/** Dark rounded card that groups one setting, such as a switch with its explanation. */
@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceRaised)
            .padding(horizontal = 30.dp, vertical = 24.dp),
        content = content,
    )
}

/** Title and explanation on the left of a [PanelCard], with [trailing] (a switch or a button) on the right. */
@Composable
fun PanelCardRow(title: String, body: String, trailing: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ivory, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = body,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = Sand,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        trailing()
    }
}

/** A 40 dp circle of [color]. The chosen accent gets an ivory ring drawn just outside it. */
@Composable
fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .drawBehind {
                if (selected) drawCircle(Ivory, radius = size.minDimension / 2 + 5.5.dp.toPx(), style = Stroke(3.dp.toPx()))
            }
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.RadioButton, onClick = onClick)
    )
}
