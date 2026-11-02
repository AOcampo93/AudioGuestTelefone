package com.olinix.audioguest.session

/** Timing rules of a guest session. Values come from the plan and the mockups. */
data class SessionConfig(
    val maxAudioSeconds: Int = 180,
    val maxVideoSeconds: Int = 120,
    val countdownFrom: Int = 3,
    /** Menu, review and gallery go back to Idle after this long without a touch. */
    val inactivitySeconds: Int = 30,
    /** How long the "¡Gracias!" screen stays before returning to Idle. */
    val savedSeconds: Int = 5,
    /**
     * Without the handset the app shows "Conecta el auricular" and records nothing.
     * Turned off only on the emulator, which cannot plug in a USB handset.
     */
    val requireHandset: Boolean = true,
) {
    fun maxSecondsFor(mode: RecordingMode): Int = when (mode) {
        RecordingMode.AUDIO -> maxAudioSeconds
        RecordingMode.VIDEO -> maxVideoSeconds
    }
}
