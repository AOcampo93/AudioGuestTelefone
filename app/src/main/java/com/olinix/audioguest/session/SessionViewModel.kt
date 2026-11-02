package com.olinix.audioguest.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.data.RecordingRepository
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import com.olinix.audioguest.lights.lightSceneFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * The guest session state machine. Screens call the event functions below;
 * every change goes through [moveTo], which also drives the lights.
 */
class SessionViewModel(
    private val lights: LightController,
    private val voiceRecorder: MessageRecorder,
    private val videoRecorder: MessageRecorder,
    private val files: RecordingFiles,
    private val repository: RecordingRepository,
    private val config: SessionConfig = SessionConfig(),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Saved messages, newest first. Loaded right away so the gallery opens already filled. */
    val recordings: StateFlow<List<Recording>> =
        repository.recordings.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Countdown, recording clock or "¡Gracias!" timer. Only one runs at a time. */
    private var timerJob: Job? = null
    private var inactivityJob: Job? = null
    private var lastScene: LightScene? = null

    /** A message is playing, so the guest is listening rather than gone. */
    private var mediaPlaying = false

    /** The file being recorded right now, until it reaches the review screen. */
    private var capturingFile: File? = null

    init {
        files.clearPending()
        moveTo(SessionState.Idle)
    }

    /** Idle → Menu. */
    fun start() {
        if (_state.value == SessionState.Idle) moveTo(SessionState.Menu)
    }

    fun back() {
        when (val current = _state.value) {
            SessionState.Menu -> moveTo(SessionState.Idle)
            is SessionState.Gallery -> moveTo(SessionState.Menu)
            is SessionState.Player -> moveTo(SessionState.Gallery(current.filter))
            is SessionState.Error -> moveTo(SessionState.Idle)
            // Mid-recording and the other screens ignore the system back gesture.
            else -> Unit
        }
    }

    fun chooseMode(mode: RecordingMode) {
        if (_state.value == SessionState.Menu) startCountdown(mode)
    }

    fun openGallery() {
        if (_state.value == SessionState.Menu) moveTo(SessionState.Gallery())
    }

    fun showTab(filter: GalleryFilter) {
        if (_state.value is SessionState.Gallery) moveTo(SessionState.Gallery(filter))
    }

    /** Gallery → Player for the tapped message. */
    fun play(recordingId: Long) {
        val gallery = _state.value as? SessionState.Gallery ?: return
        moveTo(SessionState.Player(recordingId, gallery.filter))
    }

    fun playNext() = step(1)

    fun playPrevious() = step(-1)

    /** Where a saved message's audio or video lives. */
    fun fileOf(recording: Recording): File = files.fileFor(recording)

    /** The guest pressed "Terminar" before the time ran out. */
    fun finishRecording() {
        val capturing = _state.value as? SessionState.Capturing ?: return
        // A second tap while the file is being finished changes nothing.
        if (capturingFile == null) return
        runTimer { stopCapture(capturing.mode, capturing.elapsedSeconds) }
    }

    fun save() {
        val review = _state.value as? SessionState.Review ?: return
        val kept = files.keep(review.file)
        runTimer {
            repository.add(review.mode, kept, review.durationSeconds)
            for (left in config.savedSeconds downTo 1) {
                moveTo(SessionState.Saved(secondsLeft = left, totalSeconds = config.savedSeconds))
                delay(ONE_SECOND)
            }
            moveTo(SessionState.Idle)
        }
    }

    fun retry() {
        val review = _state.value as? SessionState.Review ?: return
        files.discard(review.file)
        startCountdown(review.mode)
    }

    /** Called after the guest confirmed "Sí, descartar". */
    fun discard() {
        val review = _state.value as? SessionState.Review ?: return
        files.discard(review.file)
        moveTo(SessionState.Menu)
    }

    /** Any touch on the screen keeps the current screen alive for another [SessionConfig.inactivitySeconds]. */
    fun onUserInteraction() {
        restartInactivityTimer()
    }

    /** A message started or stopped playing. Nobody is sent back to the start in the middle of one. */
    fun onPlaybackChanged(playing: Boolean) {
        mediaPlaying = playing
        restartInactivityTimer()
    }

    /** Live microphone loudness (0..1) for the recording wave. */
    fun inputLevel(): Float {
        val capturing = _state.value as? SessionState.Capturing ?: return 0f
        return recorderFor(capturing.mode).level()
    }

    override fun onCleared() {
        for (recorder in listOf(voiceRecorder, videoRecorder)) {
            recorder.cancel()
            recorder.release()
        }
    }

    private fun recorderFor(mode: RecordingMode): MessageRecorder = when (mode) {
        RecordingMode.AUDIO -> voiceRecorder
        RecordingMode.VIDEO -> videoRecorder
    }

    private fun startCountdown(mode: RecordingMode) {
        // The camera takes a moment to open, so it starts now and its preview shows behind the numbers.
        val ready = viewModelScope.async { recorderFor(mode).prepare() }
        runTimer {
            // 3, 2, 1 and then 0, which the screen shows as "¡Ahora!".
            for (number in config.countdownFrom downTo 0) {
                moveTo(SessionState.Countdown(mode, number))
                delay(ONE_SECOND)
            }
            record(mode, ready.await())
        }
    }

    private suspend fun record(mode: RecordingMode, ready: Boolean) {
        val recorder = recorderFor(mode)
        val file = files.newPending(mode)
        if (!ready || !recorder.start(file)) {
            files.discard(file)
            recorder.release()
            val reason = if (mode == RecordingMode.VIDEO) ErrorReason.CAMERA_UNAVAILABLE else ErrorReason.MICROPHONE_UNAVAILABLE
            moveTo(SessionState.Error(reason))
            return
        }
        capturingFile = file

        val maxSeconds = config.maxSecondsFor(mode)
        for (elapsed in 0 until maxSeconds) {
            moveTo(SessionState.Capturing(mode, elapsed, maxSeconds))
            delay(ONE_SECOND)
        }
        // Time is up: the recording ends on its own.
        stopCapture(mode, maxSeconds)
    }

    private suspend fun stopCapture(mode: RecordingMode, seconds: Int) {
        val file = capturingFile ?: return
        capturingFile = null
        val recorder = recorderFor(mode)
        val complete = recorder.stop()
        recorder.release()
        if (!complete) {
            files.discard(file)
            moveTo(SessionState.Error(ErrorReason.RECORDING_FAILED))
            return
        }
        moveTo(SessionState.Review(mode, seconds, file))
    }

    /** Moves through the messages of the current tab and wraps around at either end. */
    private fun step(offset: Int) {
        val player = _state.value as? SessionState.Player ?: return
        val playlist = player.filter.pick(recordings.value)
        val index = playlist.indexOfFirst { it.id == player.recordingId }
        if (index < 0) return
        moveTo(player.copy(recordingId = playlist[(index + offset).mod(playlist.size)].id))
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
        if (mediaPlaying || !expiresWhenUnattended(_state.value)) return
        inactivityJob = viewModelScope.launch {
            delay(config.inactivitySeconds * ONE_SECOND)
            // A guest who walks away from the review did not choose to save.
            (_state.value as? SessionState.Review)?.let { files.discard(it.file) }
            moveTo(SessionState.Idle)
        }
    }

    /** Screens that wait for the guest. Countdown, recording and "¡Gracias!" end on their own. */
    private fun expiresWhenUnattended(state: SessionState): Boolean = when (state) {
        SessionState.Menu, is SessionState.Gallery, is SessionState.Review,
        is SessionState.Player, is SessionState.Error -> true
        else -> false
    }

    private companion object {
        const val ONE_SECOND = 1_000L
    }
}
