package com.olinix.audioguest.ui.host

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.settings.Accent
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.ui.idle.IdleScreen
import com.olinix.audioguest.ui.theme.Ivory
import com.olinix.audioguest.ui.theme.Muted
import com.olinix.audioguest.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ShortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Evento": type, names, date and accent color, next to a live preview of the invitation. */
@Composable
fun EventSection(
    settings: HostSettings,
    event: EventInfo,
    onChange: ((HostSettings) -> HostSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PanelLabel(stringResource(R.string.host_event_type))
        FlowRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (type in EventType.entries) {
                PanelChip(text = typeName(type), selected = type == settings.type, onClick = { onChange { it.withType(type) } })
            }
        }

        Row(Modifier.padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                NameFields(settings, onChange)
                DateField(settings.date, onPick = { date -> onChange { it.copy(date = date) } })
            }
            Column(Modifier.width(240.dp)) {
                PanelLabel(stringResource(R.string.host_preview))
                InvitationPreview(event, Modifier.padding(top = 12.dp))
                PanelLabel(stringResource(R.string.host_accent), Modifier.padding(top = 24.dp))
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (accent in Accent.entries) {
                        AccentSwatch(accent.color, selected = accent == settings.accent, onClick = { onChange { it.copy(accent = accent) } })
                    }
                }
                Text(
                    text = stringResource(R.string.host_accent_default, typeName(settings.type), accentName(settings.type.defaultAccent)),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** The names each event type asks for. Limits come from the mockup's table. */
@Composable
private fun NameFields(settings: HostSettings, onChange: ((HostSettings) -> HostSettings) -> Unit) {
    when (settings.type) {
        EventType.WEDDING -> Column {
            PanelLabel(stringResource(R.string.host_couple))
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PanelField(
                    value = settings.name1,
                    onValueChange = { name -> onChange { it.copy(name1 = name) } },
                    maxLength = HostSettings.MAX_NAME,
                    placeholder = stringResource(R.string.host_name_1),
                    modifier = Modifier.weight(1f),
                )
                Text("&", style = MaterialTheme.typography.headlineMedium, fontSize = 44.sp, color = MaterialTheme.colorScheme.primary)
                PanelField(
                    value = settings.name2,
                    onValueChange = { name -> onChange { it.copy(name2 = name) } },
                    maxLength = HostSettings.MAX_NAME,
                    placeholder = stringResource(R.string.host_name_2),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        EventType.CHRISTMAS -> LabeledField(
            label = stringResource(R.string.host_family),
            value = settings.family,
            hint = stringResource(R.string.host_family_hint),
            maxLength = HostSettings.MAX_FAMILY,
            onValueChange = { name -> onChange { it.copy(family = name) } },
        )
        EventType.BIRTHDAY -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledField(
                label = stringResource(R.string.host_birthday_name),
                value = settings.birthdayName,
                hint = stringResource(R.string.host_birthday_name_hint),
                maxLength = HostSettings.MAX_BIRTHDAY_NAME,
                onValueChange = { name -> onChange { it.copy(birthdayName = name) } },
                modifier = Modifier.weight(1f),
            )
            Column(Modifier.width(170.dp)) {
                PanelLabel(stringResource(R.string.host_age), note = stringResource(R.string.host_optional))
                PanelField(
                    value = settings.birthdayAge?.toString().orEmpty(),
                    // An empty or impossible age just means "no age": the invitation says "Cumpleaños de".
                    onValueChange = { typed -> onChange { it.copy(birthdayAge = typed.toIntOrNull()?.takeIf { age -> age in 1..HostSettings.MAX_AGE }) } },
                    maxLength = 3,
                    numeric = true,
                    placeholder = "—",
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                )
            }
        }
        EventType.COMPANY -> {
            LabeledField(
                label = stringResource(R.string.host_company_event),
                value = settings.companyEvent,
                hint = stringResource(R.string.host_company_event_hint),
                maxLength = HostSettings.MAX_TITLE,
                onValueChange = { title -> onChange { it.copy(companyEvent = title) } },
            )
            LabeledField(
                label = stringResource(R.string.host_company),
                value = settings.company,
                hint = stringResource(R.string.host_company_hint),
                maxLength = HostSettings.MAX_ORGANIZER,
                onValueChange = { name -> onChange { it.copy(company = name) } },
            )
        }
        EventType.OTHER -> {
            LabeledField(
                label = stringResource(R.string.host_other_title),
                value = settings.otherTitle,
                hint = stringResource(R.string.host_other_title_hint),
                maxLength = HostSettings.MAX_TITLE,
                onValueChange = { title -> onChange { it.copy(otherTitle = title) } },
            )
            LabeledField(
                label = stringResource(R.string.host_other_hosts),
                value = settings.otherHosts,
                hint = stringResource(R.string.host_other_hosts_hint),
                maxLength = HostSettings.MAX_ORGANIZER,
                onValueChange = { name -> onChange { it.copy(otherHosts = name) } },
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    hint: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PanelLabel(label)
        PanelField(
            value = value,
            onValueChange = onValueChange,
            maxLength = maxLength,
            placeholder = hint,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        )
    }
}

/** "14/06/2027 📅": opens the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onPick: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column {
        PanelLabel(stringResource(R.string.host_date))
        PanelButtonField(
            text = date.format(ShortDate),
            onClick = { picking = true },
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        ) {
            Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = Ivory, modifier = Modifier.size(28.dp))
        }
    }

    if (picking) {
        // The picker speaks in UTC milliseconds; the event date has no time zone.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        // The app's large type would not fit the calendar grid, so the picker keeps Material's own.
        MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = Typography()) {
            DatePickerDialog(
                onDismissRequest = { picking = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        picking = false
                    }) { Text(stringResource(R.string.host_date_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { picking = false }) { Text(stringResource(R.string.host_date_cancel)) }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }
}

/** The real invitation screen, scaled down to 30 % so the host sees exactly what guests will see. */
@Composable
private fun InvitationPreview(event: EventInfo, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.size(width = 240.dp, height = 384.dp).clip(shape)) {
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(width = 800.dp, height = 1280.dp)
                .graphicsLayer {
                    scaleX = PREVIEW_SCALE
                    scaleY = PREVIEW_SCALE
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .clearAndSetSemantics {},
        ) {
            IdleScreen(event = event, onTap = {})
        }
        Box(Modifier.matchParentSize().border(1.5.dp, Ivory.copy(alpha = 0.14f), shape))
    }
}

private const val PREVIEW_SCALE = 0.3f

@Composable
private fun typeName(type: EventType): String = stringResource(
    when (type) {
        EventType.WEDDING -> R.string.host_type_wedding
        EventType.CHRISTMAS -> R.string.host_type_christmas
        EventType.BIRTHDAY -> R.string.host_type_birthday
        EventType.COMPANY -> R.string.host_type_company
        EventType.OTHER -> R.string.host_type_other
    }
)

@Composable
private fun accentName(accent: Accent): String = stringResource(
    when (accent) {
        Accent.GOLD -> R.string.host_accent_gold
        Accent.RED -> R.string.host_accent_red
        Accent.GREEN -> R.string.host_accent_green
        Accent.BLUE -> R.string.host_accent_blue
        Accent.PINK -> R.string.host_accent_pink
    }
)
