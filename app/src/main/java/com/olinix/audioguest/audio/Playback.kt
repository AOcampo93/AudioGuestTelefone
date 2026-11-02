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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.io.File

/** Play/pause state of one recording, observable from Compose. */
@Stable
class Playback internal constructor(private val player: ExoPlayer) {

    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set

    /** Fraction already played, from 0 to 1. */
    val progress: Float
        get() = if (player.duration > 0) positionMs.toFloat() / player.duration else 0f

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
        // At the end, show the full length instead of a few milliseconds short.
        positionMs = if (player.playbackState == Player.STATE_ENDED) player.duration else player.currentPosition
    }
}

/**
 * Creates a player for [file] that lives as long as the calling screen.
 * Sound goes to the handset when it is plugged in, otherwise to the tablet speakers.
 */
@Composable
fun rememberPlayback(file: File): Playback {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            Handset.output(context)?.let(::setPreferredAudioDevice)
            prepare()
        }
    }
    val playback = remember(player) { Playback(player) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = playback.refresh()
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    // The player has no position callback, so poll it while playing.
    LaunchedEffect(playback.isPlaying) {
        while (playback.isPlaying) {
            playback.refresh()
            delay(100)
        }
    }
    return playback
}
