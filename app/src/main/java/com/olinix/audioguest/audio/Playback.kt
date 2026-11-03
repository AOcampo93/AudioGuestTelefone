package com.olinix.audioguest.audio

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.annotation.OptIn
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import kotlinx.coroutines.delay
import java.io.File

/** Play/pause state of one recording, observable from Compose. */
@Stable
class Playback internal constructor(internal val player: ExoPlayer) {

    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set

    /** Played to the end, as the welcome video needs to know to move on to the menu. */
    var ended by mutableStateOf(false)
        private set

    /** Zero until the file is loaded. Kept as state so [progress] updates the screen once it is known. */
    private var durationMs by mutableLongStateOf(0L)

    /** Fraction already played, from 0 to 1. */
    val progress: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    fun toggle() {
        when {
            player.isPlaying -> player.pause()
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }
            else -> player.play()
        }
    }

    internal fun refresh() {
        isPlaying = player.isPlaying
        ended = player.playbackState == Player.STATE_ENDED
        // The player reports an unknown length (a negative constant) until the file is loaded.
        durationMs = player.duration.coerceAtLeast(0L)
        // At the end, show the full length instead of a few milliseconds short.
        positionMs = if (player.playbackState == Player.STATE_ENDED) player.duration else player.currentPosition
    }
}

/**
 * Creates a player for [file] that lives as long as the calling screen.
 * Sound goes to the handset when it is plugged in, otherwise to the tablet speakers.
 *
 * @param autoPlay start as soon as the file is ready, as the gallery player does.
 * @param onPlayingChange told when playback starts and stops, and `false` when the screen goes away.
 * @param throughSpeaker play on the tablet's speaker even with the handset plugged in, as show mode does.
 */
// Choosing the output device is marked unstable in Media3: its signature may change between versions.
@OptIn(UnstableApi::class)
@Composable
fun rememberPlayback(
    file: File,
    autoPlay: Boolean = false,
    onPlayingChange: (Boolean) -> Unit = {},
    throughSpeaker: Boolean = false,
): Playback {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context)
            // Pause if the handset is unplugged, instead of carrying on through the tablet speakers.
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                val output = if (throughSpeaker) Handset.tabletSpeaker(context) else Handset.output(context)
                output?.let(::setPreferredAudioDevice)
                playWhenReady = autoPlay
                prepare()
            }
    }
    val playback = remember(player) { Playback(player) }
    val reportPlaying by rememberUpdatedState(onPlayingChange)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = playback.refresh()
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            reportPlaying(false)
        }
    }
    LaunchedEffect(playback, playback.isPlaying) { reportPlaying(playback.isPlaying) }
    // The player has no position callback, so poll it while playing.
    LaunchedEffect(playback.isPlaying) {
        while (playback.isPlaying) {
            playback.refresh()
            delay(100)
        }
    }
    return playback
}

/** The picture of a video [playback], letterboxed to fit unless [contentScale] says otherwise. */
@OptIn(UnstableApi::class)
@Composable
fun VideoFrame(playback: Playback, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    // A texture view, unlike a surface view, respects the rounded corners of its container.
    ContentFrame(
        player = playback.player,
        modifier = modifier,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        contentScale = contentScale,
    )
}
