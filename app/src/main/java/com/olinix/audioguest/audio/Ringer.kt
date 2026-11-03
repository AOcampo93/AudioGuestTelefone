package com.olinix.audioguest.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * The booth's bell for call mode: an old telephone's double ring, made in code so no sound file
 * is needed. It plays on the tablet's speaker even with the handset plugged in, so the room hears it.
 */
class Ringer(private val context: Context) {

    private var track: AudioTrack? = null

    /** Rings over and over until [stop]. */
    fun start() {
        if (track != null) return
        val samples = ringCycle(SAMPLE_RATE)
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                // The whole cycle is loaded once and looped, instead of being fed while it plays.
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .build()
            audioTrack.write(samples, 0, samples.size)
            audioTrack.setLoopPoints(0, samples.size, -1)
            Handset.tabletSpeaker(context)?.let(audioTrack::setPreferredDevice)
            audioTrack.play()
            track = audioTrack
        } catch (e: RuntimeException) {
            // The screen still says the booth is ringing; only the sound is missing.
            Log.e(TAG, "Could not play the ring", e)
        }
    }

    fun stop() {
        track?.run {
            stop()
            release()
        }
        track = null
    }

    private companion object {
        const val TAG = "Ringer"
        const val SAMPLE_RATE = 22_050
    }
}

/** Ring, pause, ring, then a long pause, in seconds: the classic double ring. */
private val RING_PATTERN = listOf(0.0..0.4, 0.6..1.0)
private const val CYCLE_SECONDS = 3.0

/** How often the clapper strikes the bells, and how fast each strike fades. */
private const val STRIKES_PER_SECOND = 20.0
private const val STRIKE_DECAY = 30.0

/** Fade in and out at each ring's edges, so they start and stop without a click. */
private const val EDGE_SECONDS = 0.01

/**
 * One cycle of the ring as 16-bit samples. Each ring is a bell struck [STRIKES_PER_SECOND] times
 * a second: a mix of a few high tones that fade between strikes, which gives the old-phone trill.
 */
internal fun ringCycle(sampleRate: Int): ShortArray = ShortArray((CYCLE_SECONDS * sampleRate).toInt()) { i ->
    val t = i.toDouble() / sampleRate
    val ring = RING_PATTERN.firstOrNull { t in it } ?: return@ShortArray 0
    val edge = min(t - ring.start, ring.endInclusive - t).coerceAtMost(EDGE_SECONDS) / EDGE_SECONDS
    val strike = exp(-(t % (1 / STRIKES_PER_SECOND)) * STRIKE_DECAY)
    val bell = (sin(2 * PI * 1_200 * t) + 0.6 * sin(2 * PI * 1_800 * t) + 0.3 * sin(2 * PI * 2_900 * t)) / 1.9
    (bell * strike * edge * 0.8 * Short.MAX_VALUE).toInt().toShort()
}
