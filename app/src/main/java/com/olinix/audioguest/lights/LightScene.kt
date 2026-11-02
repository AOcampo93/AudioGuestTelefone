package com.olinix.audioguest.lights

import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState

/**
 * The scenes from the "States and lights" table in the plan.
 * With WLED, each one will be a preset stored on the controller.
 */
enum class LightScene {
    /** Sweep or slow rainbow that changes every ≈10 s. */
    INVITATION,

    /** Steady warm white. */
    MENU,

    /** One red blink per number. */
    COUNTDOWN,

    /** Steady color or a slow color fade. */
    RECORDING_AUDIO,

    /** Full-brightness white as fill light. */
    RECORDING_VIDEO,

    /** Amber pulse. */
    LAST_SECONDS,

    /** Soft blue. */
    REVIEW,

    /** Green sweep. */
    SAVED,

    /** Orange blink. */
    ERROR,
}

/** Final seconds in which the recording warns in amber. */
private const val LAST_SECONDS_WARNING = 10

/** Which scene belongs to each session state. */
fun lightSceneFor(state: SessionState): LightScene = when (state) {
    SessionState.Idle -> LightScene.INVITATION
    SessionState.Menu, SessionState.Gallery -> LightScene.MENU
    is SessionState.Countdown -> LightScene.COUNTDOWN
    is SessionState.Capturing -> when {
        state.remainingSeconds <= LAST_SECONDS_WARNING -> LightScene.LAST_SECONDS
        state.mode == RecordingMode.VIDEO -> LightScene.RECORDING_VIDEO
        else -> LightScene.RECORDING_AUDIO
    }
    is SessionState.Review, is SessionState.Player -> LightScene.REVIEW
    SessionState.Saved -> LightScene.SAVED
    is SessionState.Error -> LightScene.ERROR
}
