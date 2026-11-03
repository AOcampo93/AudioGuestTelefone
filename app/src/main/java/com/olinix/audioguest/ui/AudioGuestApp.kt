package com.olinix.audioguest.ui

import androidx.activity.compose.BackHandler
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.audio.copyVideo
import com.olinix.audioguest.camera.CameraFeed
import com.olinix.audioguest.session.HostVideo
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.session.SessionViewModel
import com.olinix.audioguest.session.isHostScreen
import com.olinix.audioguest.settings.AllowedMessages
import com.olinix.audioguest.ui.countdown.CountdownScreen
import com.olinix.audioguest.ui.error.ErrorScreen
import com.olinix.audioguest.ui.gallery.GalleryScreen
import com.olinix.audioguest.ui.handset.ConnectHandsetOverlay
import com.olinix.audioguest.ui.host.HostCorner
import com.olinix.audioguest.ui.host.HostLoginScreen
import com.olinix.audioguest.ui.host.HostPanelScreen
import com.olinix.audioguest.ui.host.MessageActions
import com.olinix.audioguest.ui.host.HostVideoActions
import com.olinix.audioguest.ui.idle.IdleScreen
import com.olinix.audioguest.ui.menu.MenuScreen
import com.olinix.audioguest.ui.player.PlayerScreen
import com.olinix.audioguest.ui.recording.RecordingScreen
import com.olinix.audioguest.ui.review.ReviewScreen
import com.olinix.audioguest.ui.saved.SavedScreen
import com.olinix.audioguest.ui.saved.shareMessage
import com.olinix.audioguest.ui.show.ShowScreen
import com.olinix.audioguest.ui.hostvideo.HostVideoScreen
import com.olinix.audioguest.ui.call.RingingScreen
import com.olinix.audioguest.ui.theme.Charcoal
import kotlinx.coroutines.flow.StateFlow

/**
 * Root of the kiosk: picks the screen for the current session state.
 *
 * @param cameraPreview live front-camera image, shown behind the video countdown and recording.
 * @param onExitApp "Salir de la app" in the host panel: unpins and closes the app.
 */
@Composable
fun AudioGuestApp(
    viewModel: SessionViewModel,
    cameraPreview: StateFlow<SurfaceRequest?>,
    config: SessionConfig,
    onExitApp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hostVideos by viewModel.hostVideos.collectAsStateWithLifecycle()
    val videoProblems by viewModel.videoProblems.collectAsStateWithLifecycle()
    val microphone by viewModel.microphone.collectAsStateWithLifecycle()
    val playerSound by viewModel.playerSound.collectAsStateWithLifecycle()
    val event = eventInfoOf(settings)
    val context = LocalContext.current
    // Guests only see and hear the kinds of message the host allows.
    val guestRecordings = recordings.filter { settings.allows(it.mode) }
    // The host records their videos with the tablet's own microphone.
    val recordingMicrophone = if (state.isHostScreen) Microphone.TABLET else microphone
    val hostVideoActions = { kind: HostVideo ->
        HostVideoActions(
            video = hostVideos[kind],
            problem = videoProblems[kind],
            maxSeconds = config.maxHostVideoSeconds,
            onRecord = { viewModel.recordHostVideo(kind) },
            onImport = { uri -> viewModel.importHostVideo(kind) { target -> copyVideo(context, uri, target) } },
            onDelete = { viewModel.deleteHostVideo(kind) },
            onPlayingChange = viewModel::onPlaybackChanged,
        )
    }
    // The host does not need the handset, so the warning stays off the host screens.
    val handsetMissing = viewModel.handsetMissing.collectAsStateWithLifecycle().value && !state.isHostScreen

    // In a kiosk the system back gesture only steps back inside the guest flow.
    BackHandler { viewModel.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Charcoal)
            // Every touch, on any screen, resets the 30-second inactivity timer.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val pointerEvent = awaitPointerEvent(PointerEventPass.Initial)
                        if (pointerEvent.type == PointerEventType.Press) viewModel.onUserInteraction()
                    }
                }
            },
    ) {
        // A single viewfinder lives under both video screens, so the image does not restart
        // when the countdown turns into the recording.
        if (state.showsCamera()) CameraFeed(cameraPreview, Modifier.fillMaxSize())

        AnimatedContent(
            targetState = state,
            // The screen underneath the handset warning is blurred, as in the mockup.
            modifier = if (handsetMissing) Modifier.blur(6.dp) else Modifier,
            // Ticks inside the same screen (3 → 2, 0:41 → 0:42) must not trigger a cross-fade.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "screen",
        ) { current ->
            when (current) {
                SessionState.Idle -> IdleScreen(event = event, onTap = viewModel::start)
                SessionState.Welcome -> hostVideos[HostVideo.WELCOME]?.let { video ->
                    HostVideoScreen(
                        video = video,
                        event = event,
                        title = stringResource(R.string.welcome_chip),
                        titleIcon = R.drawable.ic_video,
                        skip = stringResource(R.string.welcome_skip),
                        onFinish = viewModel::finishWelcome,
                        onPlayingChange = viewModel::onPlaybackChanged,
                        sound = settings.welcomeSound,
                    )
                }
                SessionState.Ringing -> RingingScreen(event = event, onAnswer = viewModel::answer)
                // A call is a phone call: the question is heard through the handset.
                SessionState.CallIntro -> hostVideos[HostVideo.CALL]?.let { video ->
                    HostVideoScreen(
                        video = video,
                        event = event,
                        title = stringResource(R.string.call_question_chip),
                        titleIcon = R.drawable.ic_phone_filled,
                        skip = stringResource(R.string.call_answer_now),
                        onFinish = viewModel::finishCallIntro,
                        onPlayingChange = viewModel::onPlaybackChanged,
                    )
                }
                is SessionState.Show -> recordings.firstOrNull { it.id == current.recordingId }?.let { recording ->
                    ShowScreen(
                        recording = recording,
                        file = viewModel.fileOf(recording),
                        clipSeconds = config.showSecondsFor(recording.durationSeconds),
                        event = event,
                        onTap = viewModel::start,
                        onPlayingChange = viewModel::onPlaybackChanged,
                    )
                }
                SessionState.Menu -> MenuScreen(
                    event = event,
                    config = config,
                    onBack = viewModel::back,
                    onChoose = viewModel::chooseMode,
                    onGallery = viewModel::openGallery,
                    showGallery = settings.showMessages,
                    modes = settings.allowed.modes,
                    microphone = microphone,
                    onMicrophone = viewModel::chooseMicrophone,
                )
                is SessionState.Countdown -> CountdownScreen(current, microphone = recordingMicrophone)
                is SessionState.Capturing -> RecordingScreen(
                    state = current,
                    onFinish = viewModel::finishRecording,
                    level = viewModel::inputLevel,
                    microphone = recordingMicrophone,
                )
                is SessionState.Review -> ReviewScreen(
                    state = current,
                    onSave = viewModel::save,
                    onRetry = viewModel::retry,
                    onDiscard = viewModel::discard,
                    onPlayingChange = viewModel::onPlaybackChanged,
                )
                is SessionState.Saved -> {
                    val subject = stringResource(R.string.share_subject, event.hosts)
                    val title = stringResource(R.string.saved_share)
                    SavedScreen(current, event, onShare = {
                        current.shareFile?.let { file ->
                            viewModel.pauseForSharing()
                            shareMessage(context, file, subject, title)
                        }
                    })
                }
                is SessionState.Gallery -> GalleryScreen(
                    event = event,
                    recordings = guestRecordings,
                    filter = current.filter,
                    fileOf = viewModel::fileOf,
                    onFilter = viewModel::showTab,
                    onOpen = viewModel::play,
                    onBack = viewModel::back,
                    onLeaveMessage = viewModel::back,
                    showTabs = settings.allowed == AllowedMessages.BOTH,
                )
                is SessionState.Player -> PlayerScreen(
                    state = current,
                    recordings = guestRecordings,
                    fileOf = viewModel::fileOf,
                    onPrevious = viewModel::playPrevious,
                    onNext = viewModel::playNext,
                    onBack = viewModel::back,
                    onPlayingChange = viewModel::onPlaybackChanged,
                    sound = playerSound,
                    onSound = viewModel::choosePlayerSound,
                )
                is SessionState.Error -> ErrorScreen(reason = current.reason, onBack = viewModel::back)
                is SessionState.HostLogin -> HostLoginScreen(
                    state = current,
                    pinLength = config.hostPin.length,
                    onDigit = viewModel::typePinDigit,
                    onErase = viewModel::erasePinDigit,
                    onBack = viewModel::back,
                )
                is SessionState.HostPanel -> HostPanelScreen(
                    state = current,
                    settings = settings,
                    event = event,
                    onSection = viewModel::showHostSection,
                    onChange = viewModel::updateSettings,
                    onClose = viewModel::back,
                    onExitApp = onExitApp,
                    welcome = hostVideoActions(HostVideo.WELCOME),
                    call = hostVideoActions(HostVideo.CALL),
                    onTestCall = viewModel::testCall,
                    messages = MessageActions(
                        recordings = recordings,
                        fileOf = viewModel::fileOf,
                        onDelete = viewModel::deleteRecording,
                    ),
                )
            }
        }

        AnimatedVisibility(visible = handsetMissing, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
            ConnectHandsetOverlay()
        }

        // Above the handset warning, so the host can get in even with the handset unplugged.
        if (state == SessionState.Idle) HostCorner(onHold = viewModel::openHostLogin, modifier = Modifier.align(Alignment.TopEnd))
    }
}


private fun SessionState.showsCamera(): Boolean = when (this) {
    is SessionState.Countdown -> mode == RecordingMode.VIDEO
    is SessionState.Capturing -> mode == RecordingMode.VIDEO
    else -> false
}
