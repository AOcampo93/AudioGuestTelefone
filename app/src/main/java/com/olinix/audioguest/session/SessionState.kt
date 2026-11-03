package com.olinix.audioguest.session

import java.io.File

/** What the guest leaves. */
enum class RecordingMode { AUDIO, VIDEO }

/** Why a session ended on the error screen. */
enum class ErrorReason { MICROPHONE_UNAVAILABLE, CAMERA_UNAVAILABLE, RECORDING_FAILED }

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
        val progress: Float get() = elapsedSeconds.toFloat() / maxSeconds
    }

    /** Play back before saving: Save · Retry · Discard. [file] is still pending until saved. */
    data class Review(val mode: RecordingMode, val durationSeconds: Int, val file: File) : SessionState()

    /** "¡Gracias!" and automatic return to Idle. */
    data class Saved(val secondsLeft: Int, val totalSeconds: Int) : SessionState()

    /** "Mensajes del evento", showing the messages under one tab. */
    data class Gallery(val filter: GalleryFilter = GalleryFilter.ALL) : SessionState()

    /** One message playing. Previous and next stay inside the tab the guest came from. */
    data class Player(val recordingId: Long, val filter: GalleryFilter) : SessionState()

    /** Something failed: a clear message and what to do. */
    data class Error(val reason: ErrorReason) : SessionState()

    /** Host PIN pad, opened from a hidden corner of the invitation. [digits] says how many dots to fill. */
    data class HostLogin(val digits: Int = 0, val wrongPin: Boolean = false) : SessionState()

    /** Host panel, only reachable with the PIN. */
    data object HostPanel : SessionState()
}
