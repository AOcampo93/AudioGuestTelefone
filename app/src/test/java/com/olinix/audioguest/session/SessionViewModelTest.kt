package com.olinix.audioguest.session

import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.data.RecordingRepository
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.LightScene
import com.olinix.audioguest.settings.Accent
import com.olinix.audioguest.settings.AllowedMessages
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import com.olinix.audioguest.settings.SettingsRepository
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val lights = RecordingLights()
    private val voice = FakeRecorder()
    private val video = FakeRecorder()
    private val repository = FakeRepository()
    private val hostSettings = FakeSettings()
    private val handset = MutableStateFlow(true)
    private lateinit var recordings: File
    private lateinit var viewModel: SessionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SessionViewModel(lights, voice, video, RecordingFiles(folder.root), repository, hostSettings, handset)
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
        // Sharing is allowed by default, so "¡Gracias!" offers the file and stays a little longer.
        assertEquals(SessionState.Saved(secondsLeft = 10, totalSeconds = 10, shareFile = savedFiles().single()), viewModel.state.value)
        assertFalse(pending.exists())
        assertEquals(1, savedFiles().size)

        val saved = viewModel.recordings.value.single()
        assertEquals(RecordingMode.AUDIO, saved.mode)
        assertEquals(savedFiles().single(), viewModel.fileOf(saved))

        waitSeconds(10)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun theThanksScreenWaitsWhileTheGuestShares() = runTest(dispatcher) {
        startRecording(RecordingMode.VIDEO)
        finish()
        viewModel.save()
        waitSeconds(2)

        viewModel.pauseForSharing()
        viewModel.onAppPaused()
        waitSeconds(120)
        assertTrue(viewModel.state.value is SessionState.Saved)

        // Back from the share sheet, the countdown starts again from the top.
        viewModel.onAppResumed()
        runCurrent()
        assertEquals(10, (viewModel.state.value as SessionState.Saved).secondsLeft)
        waitSeconds(10)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun aShareSheetThatNeverOpensDoesNotStopTheCountdown() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        finish()
        viewModel.save()
        runCurrent()

        // Android refused to open it, so the app was never covered.
        viewModel.pauseForSharing()
        waitSeconds(3 + 10)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun withoutSharingTheThanksScreenIsShort() = runTest(dispatcher) {
        openHostPanel()
        viewModel.updateSettings { it.copy(allowSharing = false) }
        viewModel.back()
        startRecording(RecordingMode.AUDIO)
        finish()
        viewModel.save()
        runCurrent()
        assertEquals(SessionState.Saved(secondsLeft = 5, totalSeconds = 5), viewModel.state.value)

        viewModel.pauseForSharing()
        viewModel.onAppPaused()
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
    fun theHandsetButtonStartsASession() = runTest(dispatcher) {
        viewModel.onHandsetButton()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun unpluggingTheHandsetDropsTheRecording() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        waitSeconds(10)

        handset.value = false
        runCurrent()
        assertTrue(viewModel.handsetMissing.value)
        assertEquals(SessionState.Idle, viewModel.state.value)
        assertFalse(voice.isRecording)
        assertTrue(recordings.listFiles().isNullOrEmpty())

        handset.value = true
        runCurrent()
        assertFalse(viewModel.handsetMissing.value)
    }

    @Test
    fun unpluggingDuringTheCountdownClosesTheCamera() = runTest(dispatcher) {
        viewModel.start()
        viewModel.chooseMode(RecordingMode.VIDEO)
        runCurrent()

        handset.value = false
        runCurrent()
        assertFalse(video.isPrepared)

        // The countdown is over for good: no recording starts behind the warning.
        waitSeconds(10)
        assertEquals(SessionState.Idle, viewModel.state.value)
        assertFalse(video.isRecording)
    }

    @Test
    fun theEmulatorCanRunWithoutAHandset() = runTest(dispatcher) {
        handset.value = false
        val emulator = SessionViewModel(
            lights, voice, video, RecordingFiles(folder.root), repository, hostSettings, handset,
            SessionConfig(requireHandset = false),
        )
        runCurrent()
        assertFalse(emulator.handsetMissing.value)
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

    @Test
    fun theRightPinOpensTheHostPanel() = runTest(dispatcher) {
        viewModel.openHostLogin()
        listOf(2, 0, 2).forEach(viewModel::typePinDigit)
        assertEquals(SessionState.HostLogin(digits = 3), viewModel.state.value)

        viewModel.typePinDigit(6)
        assertEquals(SessionState.HostPanel(), viewModel.state.value)
        assertEquals(LightScene.MENU, lights.shown.last())
    }

    @Test
    fun aWrongPinClearsThePad() = runTest(dispatcher) {
        viewModel.openHostLogin()
        listOf(1, 2, 3, 4).forEach(viewModel::typePinDigit)
        assertEquals(SessionState.HostLogin(wrongPin = true), viewModel.state.value)

        // The next try starts from an empty pad, and Borrar takes back the last digit.
        listOf(2, 0, 9).forEach(viewModel::typePinDigit)
        viewModel.erasePinDigit()
        listOf(2, 6).forEach(viewModel::typePinDigit)
        assertEquals(SessionState.HostPanel(), viewModel.state.value)
    }

    @Test
    fun theHostLoginOnlyOpensFromTheInvitation() = runTest(dispatcher) {
        viewModel.start()
        viewModel.openHostLogin()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun aForgottenHostPanelClosesItself() = runTest(dispatcher) {
        openHostPanel()
        // Typing a name counts as activity, since the keyboard does not touch the screen.
        waitSeconds(100)
        viewModel.updateSettings { it.copy(name1 = "Ana María") }
        waitSeconds(100)
        assertTrue(viewModel.state.value is SessionState.HostPanel)

        waitSeconds(20)
        assertEquals(SessionState.Idle, viewModel.state.value)

        viewModel.openHostLogin()
        viewModel.back()
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun hostChangesApplyAtOnceAndAreSaved() = runTest(dispatcher) {
        openHostPanel()
        viewModel.showHostSection(HostSection.GUESTS)
        assertEquals(SessionState.HostPanel(HostSection.GUESTS), viewModel.state.value)

        viewModel.updateSettings { it.withType(EventType.CHRISTMAS) }
        assertEquals(Accent.RED, viewModel.settings.value.accent)
        runCurrent()
        assertEquals(EventType.CHRISTMAS, hostSettings.saved?.type)

        // Outside the panel nothing can change the settings.
        viewModel.back()
        viewModel.updateSettings { it.withType(EventType.COMPANY) }
        assertEquals(EventType.CHRISTMAS, viewModel.settings.value.type)
    }

    @Test
    fun savedSettingsAreLoadedAtStart() = runTest(dispatcher) {
        hostSettings.saved = HostSettings(type = EventType.BIRTHDAY, birthdayName = "Lucía")
        val restarted = SessionViewModel(lights, voice, video, RecordingFiles(folder.root), repository, hostSettings, handset)
        runCurrent()
        assertEquals("Lucía", restarted.settings.value.hosts)
    }

    @Test
    fun hiddenMessagesKeepGuestsOutOfTheGallery() = runTest(dispatcher) {
        openHostPanel()
        viewModel.updateSettings { it.copy(showMessages = false) }
        viewModel.back()

        viewModel.start()
        viewModel.openGallery()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun showModePlaysMessagesWhileNobodyComes() = runTest(dispatcher) {
        repository.saved.value = listOf(
            Recording(1, RecordingMode.AUDIO, "voice-1.m4a", durationSeconds = 40, savedAt = 1),
            Recording(2, RecordingMode.VIDEO, "video-2.mp4", durationSeconds = 25, savedAt = 2),
        )
        openHostPanel()
        viewModel.updateSettings { it.copy(showMode = true, showAfterSeconds = 30) }
        viewModel.back()

        waitSeconds(29)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(1)
        val first = viewModel.state.value as SessionState.Show
        assertEquals(LightScene.INVITATION, lights.shown.last())

        // Long messages are cut at 15 s, and the next one is a different message.
        waitSeconds(15)
        val second = viewModel.state.value as SessionState.Show
        assertNotEquals(first.recordingId, second.recordingId)

        // A touch starts a session, and the show does not come back over the menu.
        viewModel.start()
        assertEquals(SessionState.Menu, viewModel.state.value)
        waitSeconds(20)
        assertEquals(SessionState.Menu, viewModel.state.value)

        // Show mode never ends by itself, so it is switched off before the test finishes.
        viewModel.back()
        openHostPanel()
        viewModel.updateSettings { it.copy(showMode = false) }
        viewModel.back()
    }

    @Test
    fun showModeNeedsVisibleMessages() = runTest(dispatcher) {
        repository.saved.value = listOf(Recording(1, RecordingMode.AUDIO, "voice-1.m4a", durationSeconds = 40, savedAt = 1))
        openHostPanel()
        viewModel.updateSettings { it.copy(showMode = true, showMessages = false, showAfterSeconds = 30) }
        viewModel.back()

        waitSeconds(60)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun theHostRecordsAWelcomeVideoThatGuestsSeeFirst() = runTest(dispatcher) {
        openHostPanel()
        viewModel.recordHostVideo(HostVideo.WELCOME)
        runCurrent()
        waitSeconds(4)
        assertEquals(SessionState.Capturing(RecordingMode.VIDEO, 0, 30, hostVideo = HostVideo.WELCOME), viewModel.state.value)

        // It stops by itself at 30 s, and saving it goes back to the panel instead of "¡Gracias!".
        waitSeconds(30)
        assertTrue((viewModel.state.value as SessionState.Review).hostVideo == HostVideo.WELCOME)
        viewModel.save()
        assertEquals(SessionState.HostPanel(HostSection.WELCOME), viewModel.state.value)
        assertTrue(viewModel.settings.value.welcomeVideo)
        assertTrue(File(folder.root, "welcome.mp4").exists())
        assertTrue(repository.saved.value.isEmpty())

        viewModel.back()
        viewModel.start()
        assertEquals(SessionState.Welcome, viewModel.state.value)
        viewModel.finishWelcome()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun aChosenWelcomeVideoMustBeShort() = runTest(dispatcher) {
        openHostPanel()
        viewModel.importHostVideo(HostVideo.WELCOME) { target -> target.writeText("video"); 45 }
        runCurrent()
        assertEquals(VideoProblem.TOO_LONG, viewModel.videoProblems.value[HostVideo.WELCOME])
        assertNull(viewModel.hostVideos.value[HostVideo.WELCOME])

        viewModel.importHostVideo(HostVideo.WELCOME) { target -> target.writeText("video"); 18 }
        runCurrent()
        assertNull(viewModel.videoProblems.value[HostVideo.WELCOME])
        assertEquals(1, viewModel.hostVideos.value[HostVideo.WELCOME]?.version)

        viewModel.deleteHostVideo(HostVideo.WELCOME)
        assertNull(viewModel.hostVideos.value[HostVideo.WELCOME])
        assertFalse(viewModel.settings.value.welcomeVideo)
        viewModel.back()
        viewModel.start()
        assertEquals(SessionState.Menu, viewModel.state.value)
    }

    @Test
    fun theHostRecordsTheWelcomeWithoutTheHandset() = runTest(dispatcher) {
        openHostPanel()
        viewModel.recordHostVideo(HostVideo.WELCOME)
        runCurrent()
        waitSeconds(6)
        handset.value = false
        runCurrent()
        assertTrue((viewModel.state.value as SessionState.Capturing).hostVideo == HostVideo.WELCOME)
        assertTrue(video.isRecording)
    }

    @Test
    fun eachGuestPicksTheirMicrophone() = runTest(dispatcher) {
        viewModel.start()
        assertEquals(Microphone.HANDSET, viewModel.microphone.value)
        viewModel.chooseMicrophone(Microphone.TABLET)
        viewModel.chooseMode(RecordingMode.VIDEO)
        runCurrent()
        assertEquals(Microphone.TABLET, video.microphone)

        // Once recording, the choice is locked in.
        viewModel.chooseMicrophone(Microphone.HANDSET)
        assertEquals(Microphone.TABLET, viewModel.microphone.value)

        // The next guest starts again from the handset.
        waitSeconds(4)
        finish()
        viewModel.discard()
        viewModel.back()
        viewModel.start()
        assertEquals(Microphone.HANDSET, viewModel.microphone.value)
        viewModel.chooseMode(RecordingMode.AUDIO)
        runCurrent()
        assertEquals(Microphone.HANDSET, voice.microphone)
    }

    @Test
    fun theHostRecordsTheWelcomeWithTheTabletMicrophone() = runTest(dispatcher) {
        openHostPanel()
        viewModel.recordHostVideo(HostVideo.WELCOME)
        runCurrent()
        assertEquals(Microphone.TABLET, video.microphone)
    }

    @Test
    fun guestsChooseWhereTheyHearTheMessages() = runTest(dispatcher) {
        repository.saved.value = listOf(voice(id = 1))
        viewModel.start()
        // Only inside the player.
        viewModel.choosePlayerSound(SoundOutput.SPEAKER)
        assertEquals(SoundOutput.HANDSET, viewModel.playerSound.value)

        viewModel.openGallery()
        viewModel.play(recordingId = 1)
        viewModel.choosePlayerSound(SoundOutput.SPEAKER)
        assertEquals(SoundOutput.SPEAKER, viewModel.playerSound.value)

        viewModel.back()
        viewModel.back()
        viewModel.back()
        viewModel.start()
        assertEquals(SoundOutput.HANDSET, viewModel.playerSound.value)
    }

    @Test
    fun onlyTheAllowedKindCanBeRecordedSeenOrShown() = runTest(dispatcher) {
        repository.saved.value = listOf(video(id = 3), voice(id = 2), video(id = 1))
        openHostPanel()
        viewModel.updateSettings { it.copy(allowed = AllowedMessages.VIDEO_ONLY, showMode = true, showAfterSeconds = 30) }
        viewModel.back()

        // Show mode skips the voice message.
        waitSeconds(30)
        val shown = mutableSetOf<Long>()
        repeat(6) {
            shown += (viewModel.state.value as SessionState.Show).recordingId
            waitSeconds(15)
        }
        assertEquals(setOf(1L, 3L), shown)

        viewModel.start()
        viewModel.chooseMode(RecordingMode.AUDIO)
        assertEquals(SessionState.Menu, viewModel.state.value)

        // In the player, "next" goes from one video to the other.
        viewModel.openGallery()
        viewModel.play(recordingId = 3)
        viewModel.playNext()
        assertEquals(1L, (viewModel.state.value as SessionState.Player).recordingId)

        viewModel.back()
        viewModel.back()
        viewModel.back()
        openHostPanel()
        viewModel.updateSettings { it.copy(showMode = false) }
        viewModel.back()
    }

    @Test
    fun onlyTheHostDeletesMessages() = runTest(dispatcher) {
        startRecording(RecordingMode.AUDIO)
        finish()
        viewModel.save()
        waitSeconds(10)
        val saved = viewModel.recordings.value.single()
        assertTrue(viewModel.fileOf(saved).exists())

        // Guests have no way to delete; outside the panel the call does nothing.
        viewModel.deleteRecording(saved.id)
        runCurrent()
        assertEquals(1, viewModel.recordings.value.size)

        openHostPanel()
        viewModel.showHostSection(HostSection.MESSAGES)
        viewModel.deleteRecording(saved.id)
        runCurrent()
        assertTrue(viewModel.recordings.value.isEmpty())
        assertFalse(viewModel.fileOf(saved).exists())
    }

    @Test
    fun aCallIsAnsweredWithTheHandsetAndRecordsTheAnswer() = runTest(dispatcher) {
        setUpCallMode()
        // Calls come every 10 minutes, give or take a fifth: this one at the earliest, 8 minutes.
        waitSeconds(479)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(1)
        assertEquals(SessionState.Ringing, viewModel.state.value)
        assertEquals(LightScene.RINGING, lights.shown.last())

        viewModel.onHandsetButton()
        assertEquals(SessionState.CallIntro, viewModel.state.value)

        // After the question, the answer is recorded as a voice message, from the handset.
        viewModel.finishCallIntro()
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.AUDIO, 3), viewModel.state.value)
        assertEquals(Microphone.HANDSET, voice.microphone)
        waitSeconds(4)
        finish()
        viewModel.save()
        waitSeconds(10)
        assertEquals(RecordingMode.AUDIO, viewModel.recordings.value.single().mode)
        assertEquals(SessionState.Idle, viewModel.state.value)

        turnCallModeOff()
    }

    @Test
    fun anUnansweredCallRingsAgainLater() = runTest(dispatcher) {
        setUpCallMode()
        waitSeconds(480)
        assertEquals(SessionState.Ringing, viewModel.state.value)

        // Nobody came in 30 s: back to the invitation until the next call.
        waitSeconds(30)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(479)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(1)
        assertEquals(SessionState.Ringing, viewModel.state.value)

        // Turned down with the back gesture.
        viewModel.back()
        assertEquals(SessionState.Idle, viewModel.state.value)
        turnCallModeOff()
    }

    @Test
    fun aCallWaitsUntilTheBoothIsFree() = runTest(dispatcher) {
        setUpCallMode()
        waitSeconds(470)
        viewModel.start()
        waitSeconds(10)
        assertEquals(SessionState.Menu, viewModel.state.value)

        // The guest walks away at 500 s; the call tries again every minute and rings at 540 s.
        waitSeconds(20)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(39)
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(1)
        assertEquals(SessionState.Ringing, viewModel.state.value)

        viewModel.back()
        turnCallModeOff()
    }

    @Test
    fun withoutTheQuestionThereAreNoCalls() = runTest(dispatcher) {
        setUpCallMode()
        openHostPanel()
        viewModel.showHostSection(HostSection.CALL)
        viewModel.deleteHostVideo(HostVideo.CALL)
        assertFalse(viewModel.settings.value.callMode)
        viewModel.back()

        waitSeconds(3_600)
        assertEquals(SessionState.Idle, viewModel.state.value)
    }

    @Test
    fun theHostCanTestTheCallWithCallModeOff() = runTest(dispatcher) {
        openHostPanel()
        viewModel.testCall()
        assertEquals(SessionState.Idle, viewModel.state.value)
        waitSeconds(3)
        assertEquals(SessionState.Ringing, viewModel.state.value)

        // Without a question video, answering goes straight to recording.
        viewModel.answer()
        runCurrent()
        assertEquals(SessionState.Countdown(RecordingMode.AUDIO, 3), viewModel.state.value)
    }

    /** Call mode on with a question video, and a booth whose calls come at the earliest moment allowed. */
    private fun TestScope.setUpCallMode() {
        viewModel = SessionViewModel(
            lights, voice, video, RecordingFiles(folder.root), repository, hostSettings, handset, random = LowestRandom,
        )
        runCurrent()
        openHostPanel()
        viewModel.showHostSection(HostSection.CALL)
        // Picking the question switches call mode on.
        viewModel.importHostVideo(HostVideo.CALL) { target -> target.writeText("question"); 20 }
        runCurrent()
        assertTrue(viewModel.settings.value.callMode)
        viewModel.back()
    }

    /** Call mode rings forever, so it is switched off before the test finishes. */
    private fun TestScope.turnCallModeOff() {
        openHostPanel()
        viewModel.updateSettings { it.copy(callMode = false) }
        viewModel.back()
    }

    /** Always the lowest value, so the next call comes at exactly 80 % of the interval. */
    private object LowestRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }

    private fun TestScope.startRecording(mode: RecordingMode) {
        viewModel.start()
        viewModel.chooseMode(mode)
        runCurrent()
        waitSeconds(4)
    }

    /** Presses "Terminar" and lets the recorder finish the file. */
    private fun TestScope.openHostPanel() {
        viewModel.openHostLogin()
        listOf(2, 0, 2, 6).forEach(viewModel::typePinDigit)
        runCurrent()
    }

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

        override suspend fun delete(recording: Recording) {
            saved.value = saved.value.filterNot { it.id == recording.id }
        }
    }

    private class FakeSettings : SettingsRepository {
        var saved: HostSettings? = null

        override suspend fun load(): HostSettings = saved ?: HostSettings()

        override suspend fun save(settings: HostSettings) {
            saved = settings
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
        var microphone: Microphone? = null
            private set
        private var output: File? = null

        override suspend fun prepare(microphone: Microphone): Boolean {
            this.microphone = microphone
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
