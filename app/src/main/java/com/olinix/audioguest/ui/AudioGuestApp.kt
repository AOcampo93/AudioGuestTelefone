package com.olinix.audioguest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.session.SessionViewModel
import com.olinix.audioguest.ui.countdown.CountdownScreen
import com.olinix.audioguest.ui.error.ErrorScreen
import com.olinix.audioguest.ui.gallery.GalleryScreen
import com.olinix.audioguest.ui.idle.IdleScreen
import com.olinix.audioguest.ui.menu.MenuScreen
import com.olinix.audioguest.ui.player.PlayerScreen
import com.olinix.audioguest.ui.recording.RecordingScreen
import com.olinix.audioguest.ui.review.ReviewScreen
import com.olinix.audioguest.ui.saved.SavedScreen
import com.olinix.audioguest.ui.theme.Charcoal

/** Root of the kiosk: picks the screen for the current session state. */
@Composable
fun AudioGuestApp(viewModel: SessionViewModel, event: EventInfo, config: SessionConfig) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()

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
        AnimatedContent(
            targetState = state,
            // Ticks inside the same screen (3 → 2, 0:41 → 0:42) must not trigger a cross-fade.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "screen",
        ) { current ->
            when (current) {
                SessionState.Idle -> IdleScreen(event = event, onTap = viewModel::start)
                SessionState.Menu -> MenuScreen(
                    event = event,
                    config = config,
                    onBack = viewModel::back,
                    onChoose = viewModel::chooseMode,
                    onGallery = viewModel::openGallery,
                )
                is SessionState.Countdown -> CountdownScreen(current)
                is SessionState.Capturing -> RecordingScreen(
                    state = current,
                    onFinish = viewModel::finishRecording,
                    level = viewModel::inputLevel,
                )
                is SessionState.Review -> ReviewScreen(
                    state = current,
                    onSave = viewModel::save,
                    onRetry = viewModel::retry,
                    onDiscard = viewModel::discard,
                    onPlayingChange = viewModel::onPlaybackChanged,
                )
                is SessionState.Saved -> SavedScreen(current, event)
                is SessionState.Gallery -> GalleryScreen(
                    event = event,
                    recordings = recordings,
                    filter = current.filter,
                    fileOf = viewModel::fileOf,
                    onFilter = viewModel::showTab,
                    onOpen = viewModel::play,
                    onBack = viewModel::back,
                    onLeaveMessage = viewModel::back,
                )
                is SessionState.Player -> PlayerScreen(
                    state = current,
                    recordings = recordings,
                    fileOf = viewModel::fileOf,
                    onPrevious = viewModel::playPrevious,
                    onNext = viewModel::playNext,
                    onBack = viewModel::back,
                    onPlayingChange = viewModel::onPlaybackChanged,
                )
                is SessionState.Error -> ErrorScreen(reason = current.reason, onBack = viewModel::back)
            }
        }
    }
}
