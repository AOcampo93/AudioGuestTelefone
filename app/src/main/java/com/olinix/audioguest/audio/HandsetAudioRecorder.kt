package com.olinix.audioguest.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.io.File
import kotlin.math.log10

/**
 * Records voice messages as AAC (128 kbps, mono, 48 kHz) in an .m4a file,
 * from the handset microphone when it is plugged in.
 */
class HandsetAudioRecorder(private val context: Context) : MessageRecorder {

    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var level = 0f
    private var lastSampleAt = 0L

    override fun start(output: File): Boolean {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted")
            return false
        }
        val handset = Handset.input(context)
        val mediaRecorder = MediaRecorder(context)
        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(48_000)
                setAudioEncodingBitRate(128_000)
                setOutputFile(output)
                // Without this Android picks the tablet's built-in microphone.
                if (handset != null) setPreferredDevice(handset)
                prepare()
                start()
            }
            Log.i(TAG, "Recording from ${handset?.productName ?: "the built-in microphone"}")
            recorder = mediaRecorder
            this.output = output
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recording", e)
            mediaRecorder.release()
            output.delete()
            false
        }
    }

    override fun stop(): Boolean {
        val mediaRecorder = recorder ?: return false
        recorder = null
        level = 0f
        return try {
            mediaRecorder.stop()
            true
        } catch (e: RuntimeException) {
            // MediaRecorder throws when it stops before any audio was written.
            Log.e(TAG, "Recording produced no audio", e)
            output?.delete()
            false
        } finally {
            mediaRecorder.release()
        }
    }

    override fun cancel() {
        if (recorder == null) return
        stop()
        output?.delete()
    }

    override fun level(): Float {
        val mediaRecorder = recorder ?: return 0f
        val now = SystemClock.uptimeMillis()
        // getMaxAmplitude() returns the peak since the previous call, so sample at a steady pace.
        if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
            lastSampleAt = now
            val amplitude = mediaRecorder.maxAmplitude
            val loudness = if (amplitude <= 0) 0f else ((20 * log10(amplitude / MAX_AMPLITUDE) + SILENCE_DB) / SILENCE_DB).toFloat()
            // Rise instantly, fall slowly, so the wave does not flicker between words.
            level = maxOf(loudness.coerceIn(0f, 1f), level * 0.85f)
        }
        return level
    }

    private companion object {
        const val TAG = "Recorder"
        const val SAMPLE_INTERVAL_MS = 50L
        const val MAX_AMPLITUDE = 32767.0

        /** Anything quieter than -50 dB counts as silence. */
        const val SILENCE_DB = 50.0
    }
}
