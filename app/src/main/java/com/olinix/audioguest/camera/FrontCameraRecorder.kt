package com.olinix.audioguest.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.AudioSpec
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.olinix.audioguest.audio.MessageRecorder
import com.olinix.audioguest.audio.Microphone
import com.olinix.audioguest.audio.loudness
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Records video messages with the front camera: 720p portrait video with the sound from the
 * handset or the tablet's own microphone.
 *
 * The camera is only open between [prepare] and [release], so it (and Android's camera
 * indicator) stays off while nobody is recording a video.
 */
class FrontCameraRecorder(private val context: Context) : MessageRecorder {

    private val _preview = MutableStateFlow<SurfaceRequest?>(null)

    /** What the camera sees, for the viewfinder. Null while the camera is closed. */
    val preview: StateFlow<SurfaceRequest?> = _preview.asStateFlow()

    // The preview and the video both use 16:9, so the guest sees exactly what gets recorded.
    private val previewUseCase = Preview.Builder()
        .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY).build())
        .build()
        .apply { setSurfaceProvider { request -> _preview.value = request } }

    /** One video use case per microphone, since CameraX fixes where the sound comes from when it is built. */
    private val videoCaptures = mutableMapOf<Microphone, VideoCapture<Recorder>>()

    /** The use case bound between [prepare] and [release]. */
    private var videoCapture: VideoCapture<Recorder>? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var recording: Recording? = null
    private var finished: CompletableDeferred<Boolean>? = null
    private var amplitude = 0.0
    private var level = 0f
    private var lastSampleAt = 0L
    private var microphoneLogged = false

    override suspend fun prepare(microphone: Microphone): Boolean {
        val capture = videoCaptures.getOrPut(microphone) { videoCaptureFor(microphone) }
        if (cameraProvider != null && videoCapture == capture) return true
        release()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted")
            return false
        }
        val provider = ProcessCameraProvider.awaitInstance(context)
        return try {
            // Bound to the whole app rather than one screen, so the camera stays open from the
            // countdown into the recording, and Android still closes it if the app goes to the background.
            provider.bindToLifecycle(ProcessLifecycleOwner.get(), CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase, capture)
            cameraProvider = provider
            videoCapture = capture
            true
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "No front camera available", e)
            false
        }
    }

    override suspend fun start(output: File): Boolean {
        val capture = videoCapture ?: return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Microphone permission not granted")
            return false
        }
        val started = CompletableDeferred<Boolean>()
        val done = CompletableDeferred<Boolean>()
        finished = done
        amplitude = 0.0
        microphoneLogged = false
        recording = capture.output
            .prepareRecording(context, FileOutputOptions.Builder(output).build())
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> started.complete(true)
                    is VideoRecordEvent.Status -> {
                        amplitude = event.recordingStats.audioStats.audioAmplitude
                        if (!microphoneLogged) microphoneLogged = logMicrophone()
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) Log.e(TAG, "Video ended with error ${event.error}", event.cause)
                        started.complete(false)
                        done.complete(!event.hasError())
                    }
                }
            }
        // The clock on screen only starts once the first frames are really being recorded.
        val ok = withTimeoutOrNull(START_TIMEOUT_MS) { started.await() } ?: false
        if (!ok) cancel()
        return ok
    }

    override suspend fun stop(): Boolean {
        val active = recording ?: return false
        recording = null
        level = 0f
        active.stop()
        // The file is only playable once CameraX has finished writing it.
        return withTimeoutOrNull(FINISH_TIMEOUT_MS) { finished?.await() } ?: false
    }

    /** The unfinished file is left behind as "pending" and cleared on the next start. */
    override fun cancel() {
        recording?.close()
        recording = null
        level = 0f
    }

    override fun release() {
        videoCapture?.let { cameraProvider?.unbind(previewUseCase, it) }
        videoCapture = null
        cameraProvider = null
        _preview.value = null
    }

    override fun level(): Float {
        if (recording == null) return 0f
        val now = SystemClock.uptimeMillis()
        if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
            lastSampleAt = now
            // CameraX reports loudness about once a second, so glide towards each new value
            // instead of dropping to silence in between.
            level += (loudness(amplitude) - level) * 0.25f
        }
        return level
    }

    private fun videoCaptureFor(microphone: Microphone): VideoCapture<Recorder> = VideoCapture.Builder(
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            // About 30 MB per minute: a whole event fits easily on the tablet.
            .setTargetVideoEncodingBitRate(4_000_000)
            .recordFrom(microphone)
            .build()
    )
        // Mirrored like the preview, so in the review guests see themselves as they did while recording.
        .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        .build()

    /**
     * Says in the log which microphone the video is using, to confirm it is the one the guest chose.
     * Returns false while Android has not picked the device yet.
     */
    private fun logMicrophone(): Boolean {
        val device = context.getSystemService(AudioManager::class.java)
            .activeRecordingConfigurations
            .firstNotNullOfOrNull { it.audioDevice } ?: return false
        Log.i(TAG, "Video sound from ${device.productName} (device type ${device.type})")
        return true
    }

    private companion object {
        const val TAG = "VideoRecorder"
        const val START_TIMEOUT_MS = 5_000L
        const val FINISH_TIMEOUT_MS = 10_000L
        const val SAMPLE_INTERVAL_MS = 50L
    }
}

/**
 * CameraX cannot be pointed at one microphone, but Android routes its sources differently:
 * CAMCORDER (CameraX's default) always uses the tablet's own microphone, while MIC follows the
 * USB handset whenever it is plugged in, as voice messages do. setAudioSource is internal to
 * CameraX (hence the suppressed lint check), so this needs checking whenever CameraX is upgraded.
 */
@SuppressLint("RestrictedApi")
private fun Recorder.Builder.recordFrom(microphone: Microphone): Recorder.Builder = setAudioSource(
    when (microphone) {
        Microphone.HANDSET -> AudioSpec.SOURCE_MIC
        Microphone.TABLET -> AudioSpec.SOURCE_CAMCORDER
    }
)
