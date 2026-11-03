package com.olinix.audioguest.session

/**
 * The event as guests see it, already in their language.
 * The app builds it from the host settings; [Sample] is for previews.
 */
data class EventInfo(
    /** Small italic line: "Boda de", "40 años de" or the host's own title. */
    val label: String,
    /** Big line: the couple, the family, the person or the organizer. */
    val hosts: String,
    val date: String,
    /** A company event reads "Cena de fin de año · Grupo Atlas" rather than as one phrase. */
    val joinWithDot: Boolean = false,
) {
    /** One-line version for the menu and the gallery: "Boda de Ana & Miguel". */
    val headline: String
        get() = if (joinWithDot) "$label · $hosts" else "$label $hosts"

    companion object {
        val Sample = EventInfo(
            label = "Boda de",
            hosts = "Ana & Miguel",
            date = "14 de junio de 2027",
        )
    }
}
