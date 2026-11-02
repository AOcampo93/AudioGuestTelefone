package com.olinix.audioguest.util

/** Formats seconds as m:ss, the way every timer in the app is shown (0:42, 3:00). */
fun formatDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
