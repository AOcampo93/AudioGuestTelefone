package com.olinix.audioguest.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Finds the retro handset among the audio devices. Android sees it as a USB sound card;
 * a wired headset is accepted too, as the plan B if the handset turns out to be analog.
 */
object Handset {

    private val INPUT_TYPES = setOf(
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
    )

    private val OUTPUT_TYPES = INPUT_TYPES + AudioDeviceInfo.TYPE_WIRED_HEADPHONES

    /** The handset microphone, or null to fall back to the tablet's own microphone. */
    fun input(context: Context): AudioDeviceInfo? =
        audioManager(context).getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.type in INPUT_TYPES }

    /** The handset speaker, or null to fall back to the tablet's speakers. */
    fun output(context: Context): AudioDeviceInfo? =
        audioManager(context).getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type in OUTPUT_TYPES }

    /** Emits whether the handset is plugged in: once right away, then on every change. */
    fun connection(context: Context): Flow<Boolean> = callbackFlow {
        val audioManager = audioManager(context)
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = report()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = report()

            fun report() {
                val handset = input(context)
                Log.i(TAG, if (handset != null) "Handset connected: ${handset.productName} (type ${handset.type})" else "No handset connected")
                trySend(handset != null)
            }
        }
        // Registering also calls onAudioDevicesAdded with the devices already plugged in.
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
    }.distinctUntilChanged()

    private fun audioManager(context: Context) = context.getSystemService(AudioManager::class.java)

    private const val TAG = "Handset"
}
