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
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

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
    private val hostSettings: SettingsRepository,
    /** Whether the handset is plugged in, updated as it changes. */
    handsetConnected: Flow<Boolean>,
    private val config: SessionConfig = SessionConfig(),
    /** Picks the messages of show mode. Tests pass a seeded one. */
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Saved messages, newest first. Loaded right away so the gallery opens already filled. */
    val recordings: StateFlow<List<Recording>> =
        repository.recordings.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _settings = MutableStateFlow(HostSettings())

    /**
     * What the host set up in the panel. Changes apply here at once and are saved in the
     * background, so text fields never wait for the disk.
     */
    val settings: StateFlow<HostSettings> = _settings.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)

    /** False until the saved settings replace the defaults, so the app does not flash the wrong language. */
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private val _handsetMissing = MutableStateFlow(false)

    /** True while "Conecta el auricular" must cover the screen. */
    val handsetMissing: StateFlow<Boolean> = _handsetMissing.asStateFlow()

    /** Countdown, recording clock or "¡Gracias!" timer. Only one runs at a time. */
    private var timerJob: Job? = null
    private var inactivityJob: Job? = null
    private var lastScene: LightScene? = null

    /** A message is playing, so the guest is listening rather than gone. */
    private var mediaPlaying = false

    /** The file being recorded right now, until it reaches the review screen. */
    private var capturingFile: File? = null

    /** The camera opening during a video countdown. */
    private var preparing: Job? = null

    /** Digits typed on the host PIN pad. Kept here, not in the state, so the screen never holds the PIN. */
    private var typedPin = ""

    private var welcomeVersion = 0
    private val _welcomeVideo = MutableStateFlow(files.welcomeVideo.takeIf { it.exists() }?.let { WelcomeVideo(it, welcomeVersion) })

    /** The host's welcome video, or null while there is none. */
    val welcomeVideo: StateFlow<WelcomeVideo?> = _welcomeVideo.asStateFlow()

    private val _welcomeProblem = MutableStateFlow<WelcomeProblem?>(null)

    /** Why the last video the host chose was refused, shown in the panel until the next try. */
    val welcomeProblem: StateFlow<WelcomeProblem?> = _welcomeProblem.asStateFlow()

    init {
        files.clearPending()
        viewModelScope.launch {
            _settings.value = hostSettings.load()
            _settingsLoaded.value = true
            // Show mode may be on, and its countdown starts from the invitation.
            restartInactivityTimer()
        }
        moveTo(SessionState.Idle)
        viewModelScope.launch { handsetConnected.collect(::onHandsetChanged) }
    }

    /** Idle → Menu. In show mode a touch also stops the message playing and starts a session. */
    fun start() {
        if (!showsInvitation(_state.value)) return
        timerJob?.cancel()
        // The welcome video plays first, if the host turned it on and there is one.
        val welcome = _settings.value.welcomeVideo && _welcomeVideo.value != null
        moveTo(if (welcome) SessionState.Welcome else SessionState.Menu)
    }

    /** "Saltar", or the welcome video reached its end. */
    fun finishWelcome() {
        if (_state.value == SessionState.Welcome) moveTo(SessionState.Menu)
    }

    /** The button on the handset does what a touch on the screen does: it wakes the invitation. */
    fun onHandsetButton() {
        if (showsInvitation(_state.value)) start() else onUserInteraction()
    }

    fun back() {
        when (val current = _state.value) {
            is SessionState.Show -> {
                timerJob?.cancel()
                moveTo(SessionState.Idle)
            }
            SessionState.Menu -> moveTo(SessionState.Idle)
            SessionState.Welcome -> moveTo(SessionState.Menu)
            is SessionState.Gallery -> moveTo(SessionState.Menu)
            is SessionState.Player -> moveTo(SessionState.Gallery(current.filter))
            is SessionState.Error, is SessionState.HostLogin, is SessionState.HostPanel -> moveTo(SessionState.Idle)
            // Mid-recording and the other screens ignore the system back gesture.
            else -> Unit
        }
    }

    fun chooseMode(mode: RecordingMode) {
        if (_state.value == SessionState.Menu) startCountdown(mode)
    }

    fun openGallery() {
        if (_state.value == SessionState.Menu && _settings.value.showMessages) moveTo(SessionState.Gallery())
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
        runTimer { stopCapture(capturing.mode, capturing.elapsedSeconds, capturing.welcome) }
    }

    fun save() {
        val review = _state.value as? SessionState.Review ?: return
        if (review.welcome) {
            useAsWelcome(review.file)
            moveTo(SessionState.HostPanel(HostSection.WELCOME))
            return
        }
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
        startCountdown(review.mode, review.welcome)
    }

    /** Called after the guest (or the host, for the welcome video) confirmed "Sí, descartar". */
    fun discard() {
        val review = _state.value as? SessionState.Review ?: return
        files.discard(review.file)
        moveTo(if (review.welcome) SessionState.HostPanel(HostSection.WELCOME) else SessionState.Menu)
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

    /** The host held the hidden corner of the invitation. */
    fun openHostLogin() {
        if (_state.value != SessionState.Idle) return
        typedPin = ""
        moveTo(SessionState.HostLogin())
    }

    /** The PIN is checked as soon as the last digit is typed. A wrong one clears the pad. */
    fun typePinDigit(digit: Int) {
        if (_state.value !is SessionState.HostLogin) return
        typedPin += digit
        if (typedPin.length < config.hostPin.length) {
            moveTo(SessionState.HostLogin(digits = typedPin.length))
            return
        }
        val correct = typedPin == config.hostPin
        typedPin = ""
        moveTo(if (correct) SessionState.HostPanel() else SessionState.HostLogin(wrongPin = true))
    }

    fun erasePinDigit() {
        if (_state.value !is SessionState.HostLogin) return
        typedPin = typedPin.dropLast(1)
        moveTo(SessionState.HostLogin(digits = typedPin.length))
    }

    fun showHostSection(section: HostSection) {
        if (_state.value is SessionState.HostPanel) moveTo(SessionState.HostPanel(section))
    }

    /** A change made in the host panel. Typing does not touch the screen, so it also counts as activity. */
    fun updateSettings(change: (HostSettings) -> HostSettings) {
        if (_state.value !is SessionState.HostPanel) return
        changeSettings(change)
        restartInactivityTimer()
    }

    /** "Grabar con la tablet": the guests' video recording, cut at [SessionConfig.maxWelcomeSeconds]. */
    fun recordWelcome() {
        if (_state.value !is SessionState.HostPanel) return
        _welcomeProblem.value = null
        startCountdown(RecordingMode.VIDEO, welcome = true)
    }

    /**
     * "Elegir un archivo". [copy] writes the chosen video into the file it is given and returns
     * its length in seconds, or null if it could not be read. Videos over the limit are refused.
     */
    fun importWelcome(copy: suspend (File) -> Int?) {
        if (_state.value !is SessionState.HostPanel) return
        val pending = files.newPending(RecordingMode.VIDEO)
        viewModelScope.launch {
            val seconds = copy(pending)
            val problem = when {
                seconds == null -> WelcomeProblem.UNREADABLE
                seconds > config.maxWelcomeSeconds -> WelcomeProblem.TOO_LONG
                else -> null
            }
            _welcomeProblem.value = problem
            if (problem == null) useAsWelcome(pending) else files.discard(pending)
            restartInactivityTimer()
        }
    }

    fun deleteWelcome() {
        if (_state.value !is SessionState.HostPanel) return
        files.deleteWelcome()
        _welcomeVideo.value = null
        _welcomeProblem.value = null
        changeSettings { it.copy(welcomeVideo = false) }
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

    private fun changeSettings(change: (HostSettings) -> HostSettings) {
        val updated = change(_settings.value)
        _settings.value = updated
        viewModelScope.launch { hostSettings.save(updated) }
    }

    /** A host who records or picks a welcome video wants guests to see it, so it is switched on too. */
    private fun useAsWelcome(file: File) {
        files.keepAsWelcome(file)
        welcomeVersion++
        _welcomeVideo.value = WelcomeVideo(files.welcomeVideo, welcomeVersion)
        changeSettings { it.copy(welcomeVideo = true) }
    }

    private fun startCountdown(mode: RecordingMode, welcome: Boolean = false) {
        // The camera takes a moment to open, so it starts now and its preview shows behind the numbers.
        val ready = viewModelScope.async { recorderFor(mode).prepare() }
        preparing = ready
        runTimer {
            // 3, 2, 1 and then 0, which the screen shows as "¡Ahora!".
            for (number in config.countdownFrom downTo 0) {
                moveTo(SessionState.Countdown(mode, number, welcome))
                delay(ONE_SECOND)
            }
            record(mode, ready.await(), welcome)
        }
    }

    private suspend fun record(mode: RecordingMode, ready: Boolean, welcome: Boolean) {
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

        val maxSeconds = if (welcome) config.maxWelcomeSeconds else config.maxSecondsFor(mode)
        for (elapsed in 0 until maxSeconds) {
            moveTo(SessionState.Capturing(mode, elapsed, maxSeconds, welcome))
            delay(ONE_SECOND)
        }
        // Time is up: the recording ends on its own.
        stopCapture(mode, maxSeconds, welcome)
    }

    private suspend fun stopCapture(mode: RecordingMode, seconds: Int, welcome: Boolean) {
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
        moveTo(SessionState.Review(mode, seconds, file, welcome))
    }

    private fun onHandsetChanged(connected: Boolean) {
        val missing = !connected && config.requireHandset
        _handsetMissing.value = missing
        // The host records the welcome video with or without the handset.
        if (missing && !_state.value.isHostScreen) abandonRecording()
    }

    /**
     * Without the handset there is nothing to record from, and nobody can press "Terminar"
     * under the warning. A message being counted down or recorded is dropped and the session restarts.
     */
    private fun abandonRecording() {
        val mode = when (val current = _state.value) {
            is SessionState.Countdown -> current.mode
            is SessionState.Capturing -> current.mode
            else -> return
        }
        timerJob?.cancel()
        preparing?.cancel()
        val recorder = recorderFor(mode)
        recorder.cancel()
        recorder.release()
        files.discard(capturingFile)
        capturingFile = null
        moveTo(SessionState.Idle)
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
        if (mediaPlaying) return
        val state = _state.value
        inactivityJob = when {
            expiresWhenUnattended(state) -> viewModelScope.launch {
                delay((if (state.isHostScreen) config.hostInactivitySeconds else config.inactivitySeconds) * ONE_SECOND)
                // A guest who walks away from the review did not choose to save.
                (_state.value as? SessionState.Review)?.let { files.discard(it.file) }
                moveTo(SessionState.Idle)
            }
            // Nobody has come to the booth for a while: show mode plays messages to draw guests in.
            state == SessionState.Idle && _settings.value.showModeActive && recordings.value.isNotEmpty() -> viewModelScope.launch {
                delay(_settings.value.showAfterSeconds * ONE_SECOND)
                playShow()
            }
            else -> null
        }
    }

    /** Saved messages at random, each for up to [SessionConfig.showClipSeconds], until someone touches the screen. */
    private fun playShow() {
        runTimer {
            var previous: Long? = null
            while (_settings.value.showModeActive && recordings.value.isNotEmpty()) {
                // Never the same message twice in a row when there is a choice.
                val pool = recordings.value
                val next = pool.filter { it.id != previous }.ifEmpty { pool }.random(random)
                previous = next.id
                moveTo(SessionState.Show(next.id))
                delay(config.showSecondsFor(next.durationSeconds) * ONE_SECOND)
            }
            moveTo(SessionState.Idle)
        }
    }

    /** The invitation, or show mode playing over it: a touch here starts a session. */
    private fun showsInvitation(state: SessionState): Boolean = state == SessionState.Idle || state is SessionState.Show

    /**
     * Screens that wait for someone. Countdown, recording and "¡Gracias!" end on their own.
     * The host screens expire too, so a panel left open does not stay within a guest's reach.
     */
    private fun expiresWhenUnattended(state: SessionState): Boolean = when (state) {
        SessionState.Menu, is SessionState.Gallery, is SessionState.Review,
        is SessionState.Player, is SessionState.Error,
        is SessionState.HostLogin, is SessionState.HostPanel,
        // If the welcome video cannot play, guests are not left in front of a still picture.
        SessionState.Welcome -> true
        else -> false
    }

    private companion object {
        const val ONE_SECOND = 1_000L
    }
}
