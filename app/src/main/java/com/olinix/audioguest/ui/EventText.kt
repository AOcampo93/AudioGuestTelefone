package com.olinix.audioguest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.olinix.audioguest.R
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import java.time.format.DateTimeFormatter

/**
 * The invitation texts for [settings] in the current guest language. The fixed lines are
 * translated; the titles the host types for a company or another event are shown as typed.
 */
@Composable
fun eventInfoOf(settings: HostSettings): EventInfo {
    val label = when (settings.type) {
        EventType.WEDDING -> stringResource(R.string.event_label_wedding)
        EventType.CHRISTMAS -> stringResource(R.string.event_label_christmas)
        EventType.BIRTHDAY -> settings.birthdayAge
            ?.let { age -> pluralStringResource(R.plurals.event_label_birthday_age, age, age) }
            ?: stringResource(R.string.event_label_birthday)
        EventType.COMPANY -> settings.companyEvent.trim().ifEmpty { stringResource(R.string.event_label_company) }
        EventType.OTHER -> settings.otherTitle.trim().ifEmpty { stringResource(R.string.event_label_other) }
    }
    val pattern = stringResource(R.string.event_date_pattern)
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(settings.date, pattern, locale) {
        settings.date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    return EventInfo(label = label, hosts = settings.hosts, date = date, joinWithDot = settings.type == EventType.COMPANY)
}
