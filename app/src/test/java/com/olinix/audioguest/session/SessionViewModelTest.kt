package com.olinix.audioguest.session

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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val lights = RecordingLights()
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SessionViewModel(lights)
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
        assertEquals(4, lights.shown.count { it == LightScene.COUNTDOWN })
        assertEquals(LightScene.RECORDING_AUDIO, lights.shown.last())
    }

    @Test
    fun finishingEarlyKeepsTheRecordedLength() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        waitSeconds(42)

        viewModel.finishRecording()
        assertEquals(SessionState.Review(RecordingMode.AUDIO, 42), viewModel.state.value)
    }

    @Test
    fun videoStopsByItselfAtTheLimit() = runTest(dispatcher) {
        startRecording(RecordingMode.VIDEO)
        waitSeconds(111)
        assertEquals(LightScene.LAST_SECONDS, lights.shown.last())

        waitSeconds(9)
        assertEquals(SessionState.Review(RecordingMode.VIDEO, 120), viewModel.state.value)
    }

    @Test
    fun savingThanksTheGuestThenReturnsToIdle() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        viewModel.finishRecording()

        viewModel.save()
        runCurrent()
        assertEquals(SessionState.Saved(secondsLeft = 5, totalSeconds = 5), viewModel.state.value)

        waitSeconds(5)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun retryRestartsTheCountdownAndDiscardGoesToMenu() = runTest(dispatcher) {
        startRecording(RecordingMode.VIDEO)
        viewModel.finishRecording()

        viewModel.retry()
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.VIDEO, 3), viewModel.state.value)

        waitSeconds(4)
        viewModel.finishRecording()
        viewModel.discard()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

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
}
