package com.olinix.audioguest.session

/** What the guest leaves. */
enum class RecordingMode { AUDIO, VIDEO }

/**
 * Every possible moment of a guest session.
 * Single source of truth: the screen and the lights only react to this state.
 */
sealed class SessionState {

    /** Invitation screen: "Toma el auricular y toca la pantalla". */
    data object Idle : SessionState()

    /** Voice · Video · View messages. */
    data object Menu : SessionState()

    /** 3, 2, 1 and "¡Ahora!" (number 0). */
    data class Countdown(val mode: RecordingMode, val number: Int) : SessionState()

    /** Recording in progress. Named Capturing so it does not clash with the gallery's Recording data class. */
    data class Capturing(
        val mode: RecordingMode,
        val elapsedSeconds: Int,
        val maxSeconds: Int,
    ) : SessionState() {
        val remainingSeconds: Int get() = maxSeconds - elapsedSeconds
    }

    /** Play back before saving: Save · Retry · Discard. */
    data class Review(val mode: RecordingMode) : SessionState()

    /** "¡Gracias!" and automatic return to Idle. */
    data object Saved : SessionState()

    data object Gallery : SessionState()

    data class Player(val recordingId: Long) : SessionState()

    /** Something failed: a clear message and what to do. */
    data class Error(val message: String) : SessionState()
}
