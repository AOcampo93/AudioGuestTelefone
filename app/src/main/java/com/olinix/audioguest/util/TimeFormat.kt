package com.olinix.audioguest.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats seconds as m:ss, the way every timer in the app is shown (0:42, 3:00). */
fun formatDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Time of day a message was saved, 24-hour like the rest of the event signage (21:42). */
fun formatClockTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(CLOCK_TIME)

/** Day and time a message was saved, as the host panel lists them (3 nov · 21:42). Always Spanish, like the panel. */
fun formatDayAndTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DAY_AND_TIME).replace(".", "")

private val CLOCK_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// Spanish abbreviates months with a dot ("nov."), which the panel leaves out.
private val DAY_AND_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es"))
