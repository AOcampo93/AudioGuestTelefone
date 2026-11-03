package com.olinix.audioguest.audio

import java.io.File

/** Captures a guest's message into a file. */
interface MessageRecorder {

    /**
     * Gets ready ahead of [start], during the countdown, to record from [microphone].
     * The camera uses it to open and show its preview. Returns false if the device cannot be used.
     */
    suspend fun prepare(microphone: Microphone): Boolean = true

    /** Starts recording into [output]. Returns false if recording could not start. */
    suspend fun start(output: File): Boolean

    /** Stops and waits until the file is complete. Returns false if nothing usable was recorded. */
    suspend fun stop(): Boolean

    /** Stops without keeping anything, e.g. when the app is closed mid-recording. */
    fun cancel()

    /** Gives back what [prepare] took, once the recording is over. */
    fun release() {}

    /** Current input loudness from 0 (silence) to 1, for the live wave. */
    fun level(): Float
}
