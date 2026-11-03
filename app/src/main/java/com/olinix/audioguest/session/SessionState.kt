package com.olinix.audioguest.session

import java.io.File

/** What the guest leaves. */
enum class RecordingMode { AUDIO, VIDEO }

/** The parts of the host panel, in the order of its tabs. [EVENTS], the list of events, opens from the header instead. */
enum class HostSection { EVENT, WELCOME, CALL, GUESTS, MESSAGES, EVENTS }

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

    /** The host's welcome video plays before the menu; guests can skip it. */
    data object Welcome : SessionState()

    /** Call mode: the booth rings until someone answers or it gives up. */
    data object Ringing : SessionState()

    /** Whoever answered hears the host's question; their answer is recorded next. */
    data object CallIntro : SessionState()

    /**
     * 3, 2, 1 and "¡Ahora!" (number 0).
     * [hostVideo] marks the host recording one of their videos from the panel, here and in the next two states.
     */
    data class Countdown(val mode: RecordingMode, val number: Int, val hostVideo: HostVideo? = null) : SessionState()

    /** Recording in progress. Named Capturing so it does not clash with the gallery's Recording data class. */
    data class Capturing(
        val mode: RecordingMode,
        val elapsedSeconds: Int,
        val maxSeconds: Int,
        val hostVideo: HostVideo? = null,
    ) : SessionState() {
        val remainingSeconds: Int get() = maxSeconds - elapsedSeconds
        val progress: Float get() = elapsedSeconds.toFloat() / maxSeconds
    }

    /** Play back before saving: Save · Retry · Discard. [file] is still pending until saved. */
    data class Review(val mode: RecordingMode, val durationSeconds: Int, val file: File, val hostVideo: HostVideo? = null) : SessionState()

    /**
     * "¡Gracias!" and automatic return to Idle. [recordingId] is the message just saved, when the
     * host lets guests ask to receive it.
     */
    data class Saved(val secondsLeft: Int, val totalSeconds: Int, val recordingId: Long? = null) : SessionState()

    /** "Receber a minha mensagem": the guest types an e-mail or a WhatsApp number. */
    data class DeliveryForm(val recordingId: Long) : SessionState()

    /** "Pronto!", repeating where the message will go, then back to Idle. */
    data class DeliveryDone(val address: String, val secondsLeft: Int, val totalSeconds: Int) : SessionState()

    /** "Mensajes del evento", showing the messages under one tab. */
    data class Gallery(val filter: GalleryFilter = GalleryFilter.ALL) : SessionState()

    /** One message playing. Previous and next stay inside the tab the guest came from. */
    data class Player(val recordingId: Long, val filter: GalleryFilter) : SessionState()

    /** Something failed: a clear message and what to do. */
    data class Error(val reason: ErrorReason) : SessionState()

    /** Show mode: a saved message plays by itself while nobody uses the booth. */
    data class Show(val recordingId: Long) : SessionState()

    /** Host PIN pad, opened from a hidden corner of the invitation. [digits] says how many dots to fill. */
    data class HostLogin(val digits: Int = 0, val wrongPin: Boolean = false) : SessionState()

    /** Host panel, only reachable with the PIN. */
    data class HostPanel(val section: HostSection = HostSection.EVENT) : SessionState()
}

/** Screens for the host rather than guests: the PIN pad, the panel and recording the host's videos. */
val SessionState.isHostScreen: Boolean
    get() = when (this) {
        is SessionState.HostLogin, is SessionState.HostPanel -> true
        is SessionState.Countdown -> hostVideo != null
        is SessionState.Capturing -> hostVideo != null
        is SessionState.Review -> hostVideo != null
        else -> false
    }
