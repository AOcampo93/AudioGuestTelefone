package com.olinix.audioguest.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.io.File

/**
 * Records voice messages as AAC (128 kbps, mono, 48 kHz) in an .m4a file, from the handset
 * microphone or the tablet's own, whichever the guest chose.
 */
class HandsetAudioRecorder(private val context: Context) : MessageRecorder {

    private var microphone = Microphone.HANDSET
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var level = 0f
    private var lastSampleAt = 0L

    override suspend fun prepare(microphone: Microphone): Boolean {
        this.microphone = microphone
        return true
    }

    override suspend fun start(output: File): Boolean {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted")
            return false
        }
        val device = Handset.device(context, microphone)
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
                // Named explicitly: left to itself, Android may pick the other microphone.
                if (device != null) setPreferredDevice(device)
                prepare()
                start()
            }
            Log.i(TAG, "Recording from ${device?.productName ?: "the default microphone"} ($microphone chosen)")
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

    override suspend fun stop(): Boolean = finish()

    override fun cancel() {
        if (recorder == null) return
        finish()
        output?.delete()
    }

    private fun finish(): Boolean {
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

    override fun level(): Float {
        val mediaRecorder = recorder ?: return 0f
        val now = SystemClock.uptimeMillis()
        // getMaxAmplitude() returns the peak since the previous call, so sample at a steady pace.
        if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
            lastSampleAt = now
            level = smoothLevel(level, loudness(mediaRecorder.maxAmplitude / MAX_AMPLITUDE))
        }
        return level
    }

    private companion object {
        const val TAG = "VoiceRecorder"
        const val SAMPLE_INTERVAL_MS = 50L
        const val MAX_AMPLITUDE = 32767.0
    }
}
