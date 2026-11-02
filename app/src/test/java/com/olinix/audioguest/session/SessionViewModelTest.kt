package com.olinix.audioguest.session

import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.data.RecordingRepository
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val lights = RecordingLights()
    private val voice = FakeRecorder()
    private val video = FakeRecorder()
    private val repository = FakeRepository()
    private lateinit var recordings: File
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SessionViewModel(lights, voice, video, RecordingFiles(folder.root), repository)
        recordings = File(folder.root, "recordings")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun menuReturnsToIdleAfterInactivity() = runTest(dispatcher) {
        viewModel.start()
        assertEquals(SessionState.Menu, viewModel.state.value)

        waitSeconds(29)
        viewModel.onUserInteraction()
        waitSeconds(29)
        assertEquals(SessionState.Menu, viewModel.state.value)

        waitSeconds(1)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun countdownBlinksOncePerNumberThenRecords() = runTest(dispatcher) {
        viewModel.start()
        viewModel.chooseMode(RecordingMode.AUDIO)
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.AUDIO, 3), viewModel.state.value)

        waitSeconds(4)
        assertEquals(SessionState.Capturing(RecordingMode.AUDIO, 0, 180), viewModel.state.value)
        assertTrue(voice.isRecording)
        assertEquals(4, lights.shown.count { it == LightScene.COUNTDOWN })
        assertEquals(LightScene.RECORDING_AUDIO, lights.shown.last())
    }

    @Test
    fun finishingEarlyKeepsTheRecordedLength() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        waitSeconds(42)

        finish()
        val review = viewModel.state.value as SessionState.Review
        assertEquals(42, review.durationSeconds)
        assertFalse(voice.isRecording)
        assertTrue(review.file.exists())
    }

    @Test
    fun videoStopsByItselfAtTheLimit() = runTest(dispatcher) {
        startRecording(RecordingMode.VIDEO)
        waitSeconds(111)
        assertEquals(LightScene.LAST_SECONDS, lights.shown.last())

        waitSeconds(9)
        val review = viewModel.state.value as SessionState.Review
        assertEquals(120, review.durationSeconds)
        assertTrue(review.file.name.endsWith(".mp4"))
    }

    @Test
    fun theCameraIsOnlyOpenForVideo() = runTest(dispatcher) {
        viewModel.start()
        viewModel.chooseMode(RecordingMode.VIDEO)
        runCurrent()
        // It opens with the countdown, so the preview is already there at "¡Ahora!".
        assertTrue(video.isPrepared)
        assertFalse(voice.isPrepared)

        waitSeconds(4)
        assertTrue(video.isRecording)
        finish()
        assertTrue(viewModel.state.value is SessionState.Review)
        assertFalse(video.isPrepared)
    }

    @Test
    fun missingCameraShowsAnError() = runTest(dispatcher) {
        video.canPrepare = false
        startRecording(RecordingMode.VIDEO)

        assertEquals(SessionState.Error(ErrorReason.CAMERA_UNAVAILABLE), viewModel.state.value)
        assertTrue(recordings.listFiles().isNullOrEmpty())
    }

    @Test
    fun savingKeepsTheFileAndThanksTheGuest() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        finish()
        val pending = (viewModel.state.value as SessionState.Review).file

        viewModel.save()
        runCurrent()
        assertEquals(SessionState.Saved(secondsLeft = 5, totalSeconds = 5), viewModel.state.value)
        assertFalse(pending.exists())
        assertEquals(1, savedFiles().size)

        val saved = viewModel.recordings.value.single()
        assertEquals(RecordingMode.AUDIO, saved.mode)
        assertEquals(savedFiles().single(), viewModel.fileOf(saved))

        waitSeconds(5)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun retryAndDiscardDeleteTheRecording() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        finish()
        val first = (viewModel.state.value as SessionState.Review).file

        viewModel.retry()
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.AUDIO, 3), viewModel.state.value)
        assertFalse(first.exists())

        waitSeconds(4)
        finish()
        val second = (viewModel.state.value as SessionState.Review).file
        viewModel.discard()
        assertEquals(SessionState.Menu, viewModel.state.value)
        assertFalse(second.exists())
        assertTrue(savedFiles().isEmpty())
    }

    @Test
    fun walkingAwayFromTheReviewDoesNotSave() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        finish()
        val pending = (viewModel.state.value as SessionState.Review).file

        waitSeconds(30)
        assertEquals(SessionState.Idle, viewModel.state.value)
        assertFalse(pending.exists())
    }

    @Test
    fun theGalleryRemembersItsTab() = runTest(dispatcher) {
        repository.saved.value = listOf(voice(id = 2), video(id = 1))
        viewModel.start()
        viewModel.openGallery()
        viewModel.showTab(GalleryFilter.VIDEO)
        viewModel.play(recordingId = 1)
        assertEquals(SessionState.Player(1, GalleryFilter.VIDEO), viewModel.state.value)

        viewModel.back()
        assertEquals(SessionState.Gallery(GalleryFilter.VIDEO), viewModel.state.value)
        viewModel.back()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun nextAndPreviousStayInTheTabAndWrapAround() = runTest(dispatcher) {
        repository.saved.value = listOf(voice(id = 4), video(id = 3), voice(id = 2), voice(id = 1))
        runCurrent()
        viewModel.start()
        viewModel.openGallery()
        viewModel.showTab(GalleryFilter.VOICE)
        viewModel.play(recordingId = 4)

        // The video (3) is not in the "Voz" tab, so it is skipped.
        viewModel.playNext()
        assertEquals(2L, (viewModel.state.value as SessionState.Player).recordingId)
        viewModel.playNext()
        viewModel.playNext()
        assertEquals(4L, (viewModel.state.value as SessionState.Player).recordingId)
        viewModel.playPrevious()
        assertEquals(1L, (viewModel.state.value as SessionState.Player).recordingId)
    }

    @Test
    fun aPlayingMessageIsNotCutOff() = runTest(dispatcher) {
        repository.saved.value = listOf(voice(id = 1, seconds = 180))
        viewModel.start()
        viewModel.openGallery()
        viewModel.play(recordingId = 1)
        viewModel.onPlaybackChanged(playing = true)

        waitSeconds(120)
        assertEquals(SessionState.Player(1, GalleryFilter.ALL), viewModel.state.value)

        // Once it ends, the usual 30 seconds start counting.
        viewModel.onPlaybackChanged(playing = false)
        waitSeconds(30)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun missingMicrophoneShowsAnError() = runTest(dispatcher) {
        voice.canStart = false
        startRecording(RecordingMode.AUDIO)

        assertEquals(SessionState.Error(ErrorReason.MICROPHONE_UNAVAILABLE), viewModel.state.value)
        assertEquals(LightScene.ERROR, lights.shown.last())
    }

    @Test
    fun emptyRecordingShowsAnError() = runTest(dispatcher) {
        voice.producesAudio = false
        startRecording(RecordingMode.AUDIO)

        finish()
        assertEquals(SessionState.Error(ErrorReason.RECORDING_FAILED), viewModel.state.value)
        assertNull(recordings.listFiles()?.firstOrNull())
        assertEquals(LightScene.ERROR, lights.shown.last())
    }

    private fun voice(id: Long, seconds: Int = 30) = Recording(id, RecordingMode.AUDIO, "voice-$id.m4a", seconds, savedAt = id)

    private fun video(id: Long) = Recording(id, RecordingMode.VIDEO, "video-$id.mp4", 30, savedAt = id)

    private fun savedFiles(): List<File> =
        recordings.listFiles()?.filterNot { it.name.startsWith("pending-") }.orEmpty()

    private fun TestScope.startRecording(mode: RecordingMode) {
        viewModel.start()
        viewModel.chooseMode(mode)
        runCurrent()
        waitSeconds(4)
    }

    /** Presses "Terminar" and lets the recorder finish the file. */
    private fun TestScope.finish() {
        viewModel.finishRecording()
        runCurrent()
    }

    private fun TestScope.waitSeconds(seconds: Int) {
        advanceTimeBy(seconds * 1_000L)
        runCurrent()
    }

    private class RecordingLights : LightController {
        val shown = mutableListOf<LightScene>()
        override fun show(scene: LightScene) {
            shown += scene
        }
    }

    /** Keeps saved messages in a list instead of a database. */
    private class FakeRepository : RecordingRepository {
        val saved = MutableStateFlow<List<Recording>>(emptyList())
        override val recordings = saved

        override suspend fun add(mode: RecordingMode, file: File, durationSeconds: Int) {
            val id = saved.value.size + 1L
            saved.value = listOf(Recording(id, mode, file.name, durationSeconds, savedAt = id)) + saved.value
        }
    }

    /** Writes a few bytes instead of opening a microphone or a camera. */
    private class FakeRecorder : MessageRecorder {
        var canPrepare = true
        var canStart = true
        var producesAudio = true
        var isPrepared = false
            private set
        var isRecording = false
            private set
        private var output: File? = null

        override suspend fun prepare(): Boolean {
            isPrepared = canPrepare
            return canPrepare
        }

        override fun release() {
            isPrepared = false
        }

        override suspend fun start(output: File): Boolean {
            if (!canStart) return false
            output.writeText("audio")
            this.output = output
            isRecording = true
            return true
        }

        override suspend fun stop(): Boolean {
            isRecording = false
            if (!producesAudio) output?.delete()
            return producesAudio
        }

        override fun cancel() {
            isRecording = false
        }

        override fun level(): Float = if (isRecording) 0.5f else 0f
    }
}
