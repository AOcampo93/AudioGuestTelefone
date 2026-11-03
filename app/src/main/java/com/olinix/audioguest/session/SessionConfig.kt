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
    /** Longer when the guest may share the message, so there is time to find the button. */
    val savedSecondsWithSharing: Int = 10,
    /** If the share sheet has not covered the app by then, Android refused to open it. */
    val shareOpenSeconds: Int = 3,
    /**
     * Without the handset the app shows "Conecta el auricular" and records nothing.
     * Turned off only on the emulator, which cannot plug in a USB handset.
     */
    val requireHandset: Boolean = true,
    /** The host's welcome video, short so guests reach the menu quickly. */
    val maxWelcomeSeconds: Int = 30,
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
