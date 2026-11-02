package com.olinix.audioguest.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

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

    private fun audioManager(context: Context) = context.getSystemService(AudioManager::class.java)
}
