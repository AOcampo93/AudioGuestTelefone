package com.olinix.audioguest.session

/** Timing rules of a guest session. Values come from the plan and the mockups. */
data class SessionConfig(
    val maxAudioSeconds: Int = 180,
    val maxVideoSeconds: Int = 120,
    val countdownFrom: Int = 3,
    /** Menu, review and gallery go back to Idle after this long without a touch. */
    val inactivitySeconds: Int = 30,
    /** The host screens wait longer, since typing names takes a while. */
    val hostInactivitySeconds: Int = 120,
    /** How long the "¡Gracias!" screen stays before returning to Idle. */
    val savedSeconds: Int = 5,
    /** Longer when the guest may ask to receive the message, so there is time to find the button. */
    val savedSecondsWithDelivery: Int = 15,
    /** How long "Pronto!" stays after the guest left their e-mail or WhatsApp. */
    val deliveryDoneSeconds: Int = 8,
    /**
     * Without the handset the app shows "Conecta el auricular" and records nothing.
     * Turned off only on the emulator, which cannot plug in a USB handset.
     */
    val requireHandset: Boolean = true,
    /** The host's welcome and call videos, short so guests get to their own message quickly. */
    val maxHostVideoSeconds: Int = 30,
    /** How long the booth rings before giving up until the next call. */
    val ringSeconds: Int = 30,
    /** A call that comes due while someone uses the booth tries again this often. */
    val callBusyRetrySeconds: Int = 60,
    /** "Probar llamada" rings this long after the panel closes. */
    val testCallSeconds: Int = 3,
    /** Show mode plays each message for this long at most, so many guests get their turn. */
    val showClipSeconds: Int = 15,
    /** Opens the host panel. Fixed for now; the host settings will let the host change it. */
    val hostPin: String = "2026",
) {
    /** How long show mode plays a message: its length, between 3 s and [showClipSeconds]. */
    fun showSecondsFor(durationSeconds: Int): Int = durationSeconds.coerceIn(3, showClipSeconds)

    fun maxSecondsFor(mode: RecordingMode): Int = when (mode) {
        RecordingMode.AUDIO -> maxAudioSeconds
        RecordingMode.VIDEO -> maxVideoSeconds
    }
}
