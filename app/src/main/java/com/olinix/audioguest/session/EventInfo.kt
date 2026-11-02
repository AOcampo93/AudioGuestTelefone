package com.olinix.audioguest.session

/**
 * Event details shown to guests.
 * Until the host settings exist, the app uses [EventInfo.Sample].
 */
data class EventInfo(
    val label: String,
    val hosts: String,
    val date: String,
) {
    companion object {
        val Sample = EventInfo(
            label = "Boda de",
            hosts = "Ana & Miguel",
            date = "14 de junio de 2027",
        )
    }
}
