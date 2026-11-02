package com.olinix.audioguest.session

import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val recorder = FakeRecorder()
    private lateinit var recordings: File
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SessionViewModel(lights, recorder, RecordingFiles(folder.root))
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
        assertTrue(recorder.isRecording)
        assertEquals(4, lights.shown.count { it == LightScene.COUNTDOWN })
        assertEquals(LightScene.RECORDING_AUDIO, lights.shown.last())
    }

    @Test
    fun finishingEarlyKeepsTheRecordedLength() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        waitSeconds(42)

        viewModel.finishRecording()
        val review = viewModel.state.value as SessionState.Review
        assertEquals(42, review.durationSeconds)
        assertFalse(recorder.isRecording)
        assertTrue(review.file!!.exists())
    }

    @Test
    fun videoStopsByItselfAtTheLimit() = runTest(dispatcher) {
        startRecording(RecordingMode.VIDEO)
        waitSeconds(111)
        assertEquals(LightScene.LAST_SECONDS, lights.shown.last())

        waitSeconds(9)
        assertEquals(SessionState.Review(RecordingMode.VIDEO, 120, file = null), viewModel.state.value)
    }

    @Test
    fun savingKeepsTheFileAndThanksTheGuest() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        viewModel.finishRecording()
        val pending = (viewModel.state.value as SessionState.Review).file!!

        viewModel.save()
        runCurrent()
        assertEquals(SessionState.Saved(secondsLeft = 5, totalSeconds = 5), viewModel.state.value)
        assertFalse(pending.exists())
        assertEquals(1, savedFiles().size)

        waitSeconds(5)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun retryAndDiscardDeleteTheRecording() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        viewModel.finishRecording()
        val first = (viewModel.state.value as SessionState.Review).file!!

        viewModel.retry()
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.AUDIO, 3), viewModel.state.value)
        assertFalse(first.exists())

        waitSeconds(4)
        viewModel.finishRecording()
        val second = (viewModel.state.value as SessionState.Review).file!!
        viewModel.discard()
        assertEquals(SessionState.Menu, viewModel.state.value)
        assertFalse(second.exists())
        assertTrue(savedFiles().isEmpty())
    }

    @Test
    fun walkingAwayFromTheReviewDoesNotSave() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        viewModel.finishRecording()
        val pending = (viewModel.state.value as SessionState.Review).file!!

        waitSeconds(30)
        assertEquals(SessionState.Idle, viewModel.state.value)
        assertFalse(pending.exists())
    }

    @Test
    fun missingMicrophoneShowsAnError() = runTest(dispatcher) {
        recorder.canStart = false
        startRecording(RecordingMode.AUDIO)

        assertEquals(SessionState.Error(ErrorReason.MICROPHONE_UNAVAILABLE), viewModel.state.value)
        assertEquals(LightScene.ERROR, lights.shown.last())
    }

    @Test
    fun emptyRecordingShowsAnError() = runTest(dispatcher) {
        recorder.producesAudio = false
        startRecording(RecordingMode.AUDIO)

        viewModel.finishRecording()
        assertEquals(SessionState.Error(ErrorReason.RECORDING_FAILED), viewModel.state.value)
        assertNull(recordings.listFiles()?.firstOrNull())
        assertEquals(LightScene.ERROR, lights.shown.last())
    }

    private fun savedFiles(): List<File> =
        recordings.listFiles()?.filterNot { it.name.startsWith("pending-") }.orEmpty()

    private fun TestScope.startRecording(mode: RecordingMode) {
        viewModel.start()
        viewModel.chooseMode(mode)
        runCurrent()
        waitSeconds(4)
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

    /** Writes a few bytes instead of opening a microphone. */
    private class FakeRecorder : MessageRecorder {
        var canStart = true
        var producesAudio = true
        var isRecording = false
            private set
        private var output: File? = null

        override fun start(output: File): Boolean {
            if (!canStart) return false
            output.writeText("audio")
            this.output = output
            isRecording = true
            return true
        }

        override fun stop(): Boolean {
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
