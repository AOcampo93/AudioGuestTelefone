package com.olinix.audioguest.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import com.olinix.audioguest.lights.lightSceneFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The guest session state machine. Screens call the event functions below;
 * every change goes through [moveTo], which also drives the lights.
 */
class SessionViewModel(
    private val lights: LightController,
    private val config: SessionConfig = SessionConfig(),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Countdown, recording clock or "¡Gracias!" timer. Only one runs at a time. */
    private var timerJob: Job? = null
    private var inactivityJob: Job? = null
    private var lastScene: LightScene? = null

    init {
        moveTo(SessionState.Idle)
    }

    /** Idle → Menu. */
    fun start() {
        if (_state.value == SessionState.Idle) moveTo(SessionState.Menu)
    }

    fun back() {
        when (_state.value) {
            SessionState.Menu -> moveTo(SessionState.Idle)
            SessionState.Gallery -> moveTo(SessionState.Menu)
            is SessionState.Player -> moveTo(SessionState.Gallery)
            is SessionState.Error -> moveTo(SessionState.Idle)
            // Mid-recording and the other screens ignore the system back gesture.
            else -> Unit
        }
    }

    fun chooseMode(mode: RecordingMode) {
        if (_state.value == SessionState.Menu) startCountdown(mode)
    }

    fun openGallery() {
        if (_state.value == SessionState.Menu) moveTo(SessionState.Gallery)
    }

    /** The guest pressed "Terminar" before the time ran out. */
    fun finishRecording() {
        val capturing = _state.value as? SessionState.Capturing ?: return
        timerJob?.cancel()
        moveTo(SessionState.Review(capturing.mode, capturing.elapsedSeconds))
    }

    fun save() {
        if (_state.value !is SessionState.Review) return
        runTimer {
            for (left in config.savedSeconds downTo 1) {
                moveTo(SessionState.Saved(secondsLeft = left, totalSeconds = config.savedSeconds))
                delay(ONE_SECOND)
            }
            moveTo(SessionState.Idle)
        }
    }

    fun retry() {
        val review = _state.value as? SessionState.Review ?: return
        startCountdown(review.mode)
    }

    /** Called after the guest confirmed "Sí, descartar". */
    fun discard() {
        if (_state.value is SessionState.Review) moveTo(SessionState.Menu)
    }

    /** Any touch on the screen keeps the current screen alive for another [SessionConfig.inactivitySeconds]. */
    fun onUserInteraction() {
        restartInactivityTimer()
    }

    private fun startCountdown(mode: RecordingMode) = runTimer {
        // 3, 2, 1 and then 0, which the screen shows as "¡Ahora!".
        for (number in config.countdownFrom downTo 0) {
            moveTo(SessionState.Countdown(mode, number))
            delay(ONE_SECOND)
        }
        record(mode)
    }

    private suspend fun record(mode: RecordingMode) {
        val maxSeconds = config.maxSecondsFor(mode)
        for (elapsed in 0 until maxSeconds) {
            moveTo(SessionState.Capturing(mode, elapsed, maxSeconds))
            delay(ONE_SECOND)
        }
        // Time is up: the recording ends on its own.
        moveTo(SessionState.Review(mode, maxSeconds))
    }

    private fun runTimer(block: suspend () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch { block() }
    }

    private fun moveTo(next: SessionState) {
        _state.value = next

        // The countdown blinks once per number, so it is sent every second;
        // the other scenes are only sent when they change.
        val scene = lightSceneFor(next)
        if (scene != lastScene || next is SessionState.Countdown) {
            lights.show(scene)
            lastScene = scene
        }
        restartInactivityTimer()
    }

    private fun restartInactivityTimer() {
        inactivityJob?.cancel()
        if (!expiresWhenUnattended(_state.value)) return
        inactivityJob = viewModelScope.launch {
            delay(config.inactivitySeconds * ONE_SECOND)
            moveTo(SessionState.Idle)
        }
    }

    /** Screens that wait for the guest. Countdown, recording and "¡Gracias!" end on their own. */
    private fun expiresWhenUnattended(state: SessionState): Boolean = when (state) {
        SessionState.Menu, SessionState.Gallery, is SessionState.Review,
        is SessionState.Player, is SessionState.Error -> true
        else -> false
    }

    private companion object {
        const val ONE_SECOND = 1_000L
    }
}
