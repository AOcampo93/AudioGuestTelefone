package com.olinix.audioguest.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olinix.audioguest.audio.EventStorage
import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.data.Delivery
import com.olinix.audioguest.data.DeliveryChannel
import com.olinix.audioguest.data.DeliveryRepository
import com.olinix.audioguest.data.Event
import com.olinix.audioguest.data.EventRepository
import com.olinix.audioguest.data.EventSummary
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.data.RecordingRepository
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import com.olinix.audioguest.lights.lightSceneFor
import com.olinix.audioguest.settings.HostSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

/**
 * The guest session state machine. Screens call the event functions below;
 * every change goes through [moveTo], which also drives the lights.
 * Everything happens inside the active event: its settings, its messages and its videos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val lights: LightController,
    private val voiceRecorder: MessageRecorder,
    private val videoRecorder: MessageRecorder,
    private val storage: EventStorage,
    private val repository: RecordingRepository,
    private val events: EventRepository,
    private val deliveries: DeliveryRepository,
    /** Whether the handset is plugged in, updated as it changes. */
    handsetConnected: Flow<Boolean>,
    private val config: SessionConfig = SessionConfig(),
    /** Picks the messages of show mode. Tests pass a seeded one. */
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** The event the booth records into, once loaded. */
    private var activeEvent: Event? = null
    private val _activeEventId = MutableStateFlow<Long?>(null)

    /** Which event is active, for the panel's list. */
    val activeEventId: StateFlow<Long?> = _activeEventId.asStateFlow()

    /** The active event's folders. Set as soon as the event loads, before anyone can record. */
    private lateinit var files: RecordingFiles

    /** The active event's saved messages, newest first. Loaded right away so the gallery opens already filled. */
    val recordings: StateFlow<List<Recording>> = _activeEventId.filterNotNull()
        .flatMapLatest { id -> repository.recordings(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Who asked to receive which message in the active event, oldest first. */
    val deliveryList: StateFlow<List<Delivery>> = _activeEventId.filterNotNull()
        .flatMapLatest { id -> deliveries.deliveries(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every event, newest first, for the panel. */
    val eventList: StateFlow<List<EventSummary>> = events.summaries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _settings = MutableStateFlow(HostSettings())

    /**
     * What the host set up in the panel for the active event. Changes apply here at once and are
     * saved in the background, so text fields never wait for the disk.
     */
    val settings: StateFlow<HostSettings> = _settings.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)

    /** False until the active event replaces the defaults, so the app does not flash the wrong language. */
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private val _handsetMissing = MutableStateFlow(false)

    /** True while "Conecta el auricular" must cover the screen. */
    val handsetMissing: StateFlow<Boolean> = _handsetMissing.asStateFlow()

    private val _microphone = MutableStateFlow(Microphone.HANDSET)

    /** The microphone this guest chose in the menu. Every new guest starts with the handset. */
    val microphone: StateFlow<Microphone> = _microphone.asStateFlow()

    private val _playerSound = MutableStateFlow(SoundOutput.HANDSET)

    /** Where this guest hears the saved messages. Every new guest starts with the handset. */
    val playerSound: StateFlow<SoundOutput> = _playerSound.asStateFlow()

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

    /** Bumped each time the host replaces a video, so its frame reloads. */
    private var hostVideoVersion = 0
    private val _hostVideos = MutableStateFlow<Map<HostVideo, HostVideoFile>>(emptyMap())

    /** The host's welcome and call videos that exist on the tablet. */
    val hostVideos: StateFlow<Map<HostVideo, HostVideoFile>> = _hostVideos.asStateFlow()

    private val _videoProblems = MutableStateFlow<Map<HostVideo, VideoProblem>>(emptyMap())

    /** Why the last video the host picked for each place was refused, shown in the panel until the next try. */
    val videoProblems: StateFlow<Map<HostVideo, VideoProblem>> = _videoProblems.asStateFlow()

    /** Waits for the next call of call mode, then rings. */
    private var callJob: Job? = null

    init {
        viewModelScope.launch { useEvent(events.active()) }
        moveTo(SessionState.Idle)
        viewModelScope.launch { handsetConnected.collect(::onHandsetChanged) }
    }

    /** Idle → Menu. In show mode a touch also stops the message playing and starts a session. */
    fun start() {
        if (!showsInvitation(_state.value) || !_settingsLoaded.value) return
        timerJob?.cancel()
        _microphone.value = Microphone.HANDSET
        _playerSound.value = SoundOutput.HANDSET
        // The welcome video plays first, if the host turned it on and there is one.
        val welcome = _settings.value.welcomeVideo && HostVideo.WELCOME in _hostVideos.value
        moveTo(if (welcome) SessionState.Welcome else SessionState.Menu)
    }

    /** "Contestar", a tap anywhere or the handset button while the booth rings: the host's question plays first. */
    fun answer() {
        if (_state.value != SessionState.Ringing) return
        timerJob?.cancel()
        _microphone.value = Microphone.HANDSET
        _playerSound.value = SoundOutput.HANDSET
        scheduleCall()
        moveTo(SessionState.CallIntro)
    }

    /** The question ended, or the guest skipped it: their answer is recorded right away, with no menu. */
    fun finishCallIntro() {
        if (_state.value == SessionState.CallIntro) startCountdown(_settings.value.callAnswerMode)
    }

    /** "Saltar", or the welcome video reached its end. */
    fun finishWelcome() {
        if (_state.value == SessionState.Welcome) moveTo(SessionState.Menu)
    }

    /** The button on the handset does what a touch on the screen does: it wakes the invitation or answers the call. */
    fun onHandsetButton() {
        when {
            _state.value == SessionState.Ringing -> answer()
            showsInvitation(_state.value) -> start()
            else -> onUserInteraction()
        }
    }

    fun back() {
        when (val current = _state.value) {
            is SessionState.Show -> {
                timerJob?.cancel()
                moveTo(SessionState.Idle)
            }
            SessionState.Menu, SessionState.CallIntro, is SessionState.DeliveryForm -> moveTo(SessionState.Idle)
            SessionState.Welcome -> moveTo(SessionState.Menu)
            // Turning the call down: it rings again at the next call.
            SessionState.Ringing -> {
                timerJob?.cancel()
                moveTo(SessionState.Idle)
                scheduleCall()
            }
            is SessionState.Gallery -> moveTo(SessionState.Menu)
            is SessionState.Player -> moveTo(SessionState.Gallery(current.filter))
            is SessionState.Error, is SessionState.HostLogin, is SessionState.HostPanel -> moveTo(SessionState.Idle)
            // Mid-recording and the other screens ignore the system back gesture.
            else -> Unit
        }
    }

    fun chooseMode(mode: RecordingMode) {
        if (_state.value == SessionState.Menu && _settings.value.allows(mode)) startCountdown(mode)
    }

    fun chooseMicrophone(microphone: Microphone) {
        if (_state.value == SessionState.Menu) _microphone.value = microphone
    }

    /** "Auricular" or "Altavoz" wherever a message plays back. It keeps playing, now through the other one. */
    fun choosePlayerSound(sound: SoundOutput) {
        if (_state.value is SessionState.Player || _state.value is SessionState.Review) _playerSound.value = sound
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
        runTimer { stopCapture(capturing.mode, capturing.elapsedSeconds, capturing.hostVideo) }
    }

    fun save() {
        val review = _state.value as? SessionState.Review ?: return
        review.hostVideo?.let { kind ->
            useAsHostVideo(review.file, kind)
            moveTo(SessionState.HostPanel(kind.section))
            return
        }
        val eventId = activeEvent?.id ?: return
        val kept = files.keep(review.file)
        val offerDelivery = _settings.value.allowSharing
        runTimer {
            val recording = repository.add(eventId, review.mode, kept, review.durationSeconds)
            if (offerDelivery) thankGuest(config.savedSecondsWithDelivery, recording.id) else thankGuest(config.savedSeconds, null)
        }
    }

    /** "Receber a minha mensagem" on the thanks screen. */
    fun askForDelivery() {
        val saved = _state.value as? SessionState.Saved ?: return
        val recordingId = saved.recordingId ?: return
        timerJob?.cancel()
        moveTo(SessionState.DeliveryForm(recordingId))
    }

    /**
     * "Enviar": keeps who wants the message, for the host to send by hand later. Returns false,
     * keeping the form open, when [address] does not look like an e-mail or a phone number.
     */
    fun submitDelivery(channel: DeliveryChannel, address: String, wantsNews: Boolean): Boolean {
        val form = _state.value as? SessionState.DeliveryForm ?: return false
        if (!channel.accepts(address)) return false
        val event = activeEvent ?: return false
        val recording = recordings.value.firstOrNull { it.id == form.recordingId } ?: return false
        val typed = address.trim()
        viewModelScope.launch {
            deliveries.add(
                Delivery(
                    eventId = event.id,
                    recordingId = recording.id,
                    fileName = recording.fileName,
                    channel = channel,
                    address = typed,
                    wantsNews = wantsNews,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
        runTimer {
            for (left in config.deliveryDoneSeconds downTo 1) {
                moveTo(SessionState.DeliveryDone(typed, left, config.deliveryDoneSeconds))
                delay(ONE_SECOND)
            }
            moveTo(SessionState.Idle)
        }
        return true
    }

    fun retry() {
        val review = _state.value as? SessionState.Review ?: return
        files.discard(review.file)
        startCountdown(review.mode, review.hostVideo)
    }

    /** Called after the guest (or the host, for one of their videos) confirmed "Sí, descartar". */
    fun discard() {
        val review = _state.value as? SessionState.Review ?: return
        files.discard(review.file)
        moveTo(review.hostVideo?.let { SessionState.HostPanel(it.section) } ?: SessionState.Menu)
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
        if (_state.value != SessionState.Idle || !_settingsLoaded.value) return
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

    /** "Grabar con la tablet": the guests' video recording, cut at [SessionConfig.maxHostVideoSeconds]. */
    fun recordHostVideo(kind: HostVideo) {
        if (_state.value !is SessionState.HostPanel) return
        _videoProblems.value -= kind
        startCountdown(RecordingMode.VIDEO, hostVideo = kind)
    }

    /**
     * "Elegir un archivo". [copy] writes the chosen video into the file it is given and returns
     * its length in seconds, or null if it could not be read. Videos over the limit are refused.
     */
    fun importHostVideo(kind: HostVideo, copy: suspend (File) -> Int?) {
        if (_state.value !is SessionState.HostPanel) return
        val pending = files.newPending(RecordingMode.VIDEO)
        viewModelScope.launch {
            val seconds = copy(pending)
            val problem = when {
                seconds == null -> VideoProblem.UNREADABLE
                seconds > config.maxHostVideoSeconds -> VideoProblem.TOO_LONG
                else -> null
            }
            _videoProblems.value = if (problem == null) _videoProblems.value - kind else _videoProblems.value + (kind to problem)
            if (problem == null) useAsHostVideo(pending, kind) else files.discard(pending)
            restartInactivityTimer()
        }
    }

    /** Without its video, the welcome is skipped and call mode turns off. */
    fun deleteHostVideo(kind: HostVideo) {
        if (_state.value !is SessionState.HostPanel) return
        files.deleteHostVideo(kind)
        _hostVideos.value -= kind
        _videoProblems.value -= kind
        changeSettings { it.withHostVideo(kind, on = false) }
    }

    /** "Probar llamada": the panel closes and the booth rings a moment later, even with call mode off. It needs the question. */
    fun testCall() {
        if (_state.value !is SessionState.HostPanel || HostVideo.CALL !in _hostVideos.value) return
        moveTo(SessionState.Idle)
        callJob?.cancel()
        callJob = viewModelScope.launch {
            delay(config.testCallSeconds * ONE_SECOND)
            if (showsInvitation(_state.value)) ring()
        }
    }

    /** The host ticked a request in Mensajes as sent, or back to "to send". */
    fun markDelivered(deliveryId: Long, sent: Boolean) {
        if (_state.value !is SessionState.HostPanel) return
        viewModelScope.launch { deliveries.setSent(deliveryId, sent) }
        restartInactivityTimer()
    }

    /** "Usar" in the panel's list of events: the booth records into that one from now on. */
    fun switchEvent(eventId: Long) {
        if (_state.value !is SessionState.HostPanel || eventId == activeEvent?.id) return
        viewModelScope.launch {
            val event = events.get(eventId) ?: return@launch
            events.activate(eventId)
            useEvent(event)
        }
    }

    /** "Novo evento": created from the active one's way of working, then opened at its names and date. */
    fun createEvent() {
        if (_state.value !is SessionState.HostPanel) return
        viewModelScope.launch {
            val event = events.create(_settings.value.forNewEvent(LocalDate.now()))
            events.activate(event.id)
            useEvent(event)
            moveTo(SessionState.HostPanel(HostSection.EVENT))
        }
    }

    /** Deletes a past event with its messages and videos, once the host confirmed. The active one cannot go. */
    fun deleteEvent(eventId: Long) {
        if (_state.value !is SessionState.HostPanel || eventId == activeEvent?.id) return
        viewModelScope.launch {
            events.delete(eventId)
            storage.delete(eventId)
        }
        restartInactivityTimer()
    }

    /** "Eliminar" in the panel's list of messages, once the host confirmed. Its file goes too. */
    fun deleteRecording(recordingId: Long) {
        if (_state.value !is SessionState.HostPanel) return
        val recording = recordings.value.firstOrNull { it.id == recordingId } ?: return
        viewModelScope.launch {
            repository.delete(recording)
            deliveries.deleteForRecording(recording.id)
            files.discard(files.fileFor(recording))
        }
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

    /**
     * Makes [event] the one the booth works for: its settings, folders and videos. Show mode and
     * call mode start over with its own settings.
     */
    private fun useEvent(event: Event) {
        activeEvent = event
        files = storage.filesFor(event.id)
        // Leftovers of a session that was interrupted (app closed, power cut).
        files.clearPending()
        _settings.value = event.settings
        hostVideoVersion++
        _hostVideos.value = HostVideo.entries
            .mapNotNull { kind -> files.hostVideo(kind).takeIf { it.exists() }?.let { kind to HostVideoFile(it, hostVideoVersion) } }
            .toMap()
        _videoProblems.value = emptyMap()
        _activeEventId.value = event.id
        _settingsLoaded.value = true
        restartInactivityTimer()
        scheduleCall()
    }

    private fun changeSettings(change: (HostSettings) -> HostSettings) {
        val event = activeEvent ?: return
        val previous = _settings.value
        val updated = change(previous)
        _settings.value = updated
        activeEvent = event.copy(settings = updated)
        viewModelScope.launch { events.save(event.copy(settings = updated)) }
        if (updated.callMode != previous.callMode || updated.callEveryMinutes != previous.callEveryMinutes) scheduleCall()
    }

    /** A host who records or picks a video wants guests to see it, so its welcome or call is switched on too. */
    private fun useAsHostVideo(file: File, kind: HostVideo) {
        files.keepAsHostVideo(file, kind)
        hostVideoVersion++
        _hostVideos.value += kind to HostVideoFile(files.hostVideo(kind), hostVideoVersion)
        changeSettings { it.withHostVideo(kind, on = true) }
    }

    private fun HostSettings.withHostVideo(kind: HostVideo, on: Boolean): HostSettings = when (kind) {
        HostVideo.WELCOME -> copy(welcomeVideo = on)
        HostVideo.CALL -> copy(callMode = on)
    }

    /** Call mode needs the question to play when someone answers. */
    private fun callModeActive(): Boolean = _settings.value.callMode && HostVideo.CALL in _hostVideos.value

    /** Plans the next call [delaySeconds] from now, replacing the one planned before. Off, nothing is planned. */
    private fun scheduleCall(delaySeconds: Int = nextCallDelay()) {
        callJob?.cancel()
        if (!callModeActive()) return
        callJob = viewModelScope.launch {
            delay(delaySeconds * ONE_SECOND)
            // Never over someone using the booth, nor with the handset unplugged: try again in a while.
            while (!showsInvitation(_state.value) || _handsetMissing.value) delay(config.callBusyRetrySeconds * ONE_SECOND)
            ring()
        }
    }

    /** The host's interval, give or take a fifth, so the calls do not feel mechanical. */
    private fun nextCallDelay(): Int = (_settings.value.callEveryMinutes * 60 * random.nextDouble(0.8, 1.2)).toInt()

    /** Rings for [SessionConfig.ringSeconds]. Unanswered, the booth goes back to the invitation and waits for the next call. */
    private fun ring() {
        runTimer {
            moveTo(SessionState.Ringing)
            delay(config.ringSeconds * ONE_SECOND)
            moveTo(SessionState.Idle)
            scheduleCall()
        }
    }

    /** "¡Gracias!" for [seconds], then back to the invitation. With a [recordingId], the guest may ask to receive it. */
    private suspend fun thankGuest(seconds: Int, recordingId: Long?) {
        for (left in seconds downTo 1) {
            moveTo(SessionState.Saved(secondsLeft = left, totalSeconds = seconds, recordingId = recordingId))
            delay(ONE_SECOND)
        }
        moveTo(SessionState.Idle)
    }

    private fun startCountdown(mode: RecordingMode, hostVideo: HostVideo? = null) {
        // The host records from the tablet, standing in front of it rather than holding the handset.
        val microphone = if (hostVideo != null) Microphone.TABLET else _microphone.value
        // The camera takes a moment to open, so it starts now and its preview shows behind the numbers.
        val ready = viewModelScope.async { recorderFor(mode).prepare(microphone) }
        preparing = ready
        runTimer {
            // 3, 2, 1 and then 0, which the screen shows as "¡Ahora!".
            for (number in config.countdownFrom downTo 0) {
                moveTo(SessionState.Countdown(mode, number, hostVideo))
                delay(ONE_SECOND)
            }
            record(mode, ready.await(), hostVideo)
        }
    }

    private suspend fun record(mode: RecordingMode, ready: Boolean, hostVideo: HostVideo?) {
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

        val maxSeconds = if (hostVideo != null) config.maxHostVideoSeconds else config.maxSecondsFor(mode)
        for (elapsed in 0 until maxSeconds) {
            moveTo(SessionState.Capturing(mode, elapsed, maxSeconds, hostVideo))
            delay(ONE_SECOND)
        }
        // Time is up: the recording ends on its own.
        stopCapture(mode, maxSeconds, hostVideo)
    }

    private suspend fun stopCapture(mode: RecordingMode, seconds: Int, hostVideo: HostVideo?) {
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
        moveTo(SessionState.Review(mode, seconds, file, hostVideo))
    }

    private fun onHandsetChanged(connected: Boolean) {
        val missing = !connected && config.requireHandset
        _handsetMissing.value = missing
        // The host records their videos with or without the handset.
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
        val playlist = player.filter.pick(guestRecordings())
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
            state == SessionState.Idle && _settings.value.showModeActive && guestRecordings().isNotEmpty() -> viewModelScope.launch {
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
            while (_settings.value.showModeActive && guestRecordings().isNotEmpty()) {
                // Never the same message twice in a row when there is a choice.
                val pool = guestRecordings()
                val next = pool.filter { it.id != previous }.ifEmpty { pool }.random(random)
                previous = next.id
                moveTo(SessionState.Show(next.id))
                delay(config.showSecondsFor(next.durationSeconds) * ONE_SECOND)
            }
            moveTo(SessionState.Idle)
        }
    }

    /** The saved messages of the kinds the host allows, the only ones guests see and hear. */
    private fun guestRecordings(): List<Recording> = recordings.value.filter { _settings.value.allows(it.mode) }

    /** The invitation, or show mode playing over it: a touch here starts a session. */
    private fun showsInvitation(state: SessionState): Boolean = state == SessionState.Idle || state is SessionState.Show

    /**
     * Screens that wait for someone. Countdown, recording and "¡Gracias!" end on their own.
     * The host screens expire too, so a panel left open does not stay within a guest's reach.
     */
    private fun expiresWhenUnattended(state: SessionState): Boolean = when (state) {
        SessionState.Menu, is SessionState.Gallery, is SessionState.Review, is SessionState.DeliveryForm,
        is SessionState.Player, is SessionState.Error,
        is SessionState.HostLogin, is SessionState.HostPanel,
        // If the host's video cannot play, guests are not left in front of a still picture.
        SessionState.Welcome, SessionState.CallIntro -> true
        else -> false
    }

    private companion object {
        const val ONE_SECOND = 1_000L
    }
}
