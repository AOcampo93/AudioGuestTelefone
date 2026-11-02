package com.olinix.audioguest.audio

import java.io.File

/** Captures a guest's message into a file. */
interface MessageRecorder {

    /** Starts recording into [output]. Returns false if the microphone could not be opened. */
    fun start(output: File): Boolean

    /** Stops and finalizes the file. Returns false if nothing usable was recorded. */
    fun stop(): Boolean

    /** Stops without keeping anything, e.g. when the app is closed mid-recording. */
    fun cancel()

    /** Current input loudness from 0 (silence) to 1, for the live wave. */
    fun level(): Float
}
