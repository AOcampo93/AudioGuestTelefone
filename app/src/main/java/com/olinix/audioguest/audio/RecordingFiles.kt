package com.olinix.audioguest.audio

import com.olinix.audioguest.session.RecordingMode
import java.io.File

/**
 * Where recordings live on the tablet. A new recording is "pending" until the guest presses
 * Guardar; pending files are deleted on retry, discard or when the guest walks away.
 */
class RecordingFiles(root: File) {

    private val directory = File(root, "recordings").apply { mkdirs() }

    fun newPending(mode: RecordingMode): File =
        File(directory, "$PENDING_PREFIX${prefix(mode)}-${System.currentTimeMillis()}.${extension(mode)}")

    /** Turns a pending file into a kept one and returns its final location. */
    fun keep(pending: File): File {
        val kept = File(directory, pending.name.removePrefix(PENDING_PREFIX))
        if (!pending.renameTo(kept)) pending.copyTo(kept, overwrite = true).also { pending.delete() }
        return kept
    }

    fun discard(file: File?) {
        file?.delete()
    }

    /** Removes leftovers from a session that was interrupted (app closed, power cut). */
    fun clearPending() {
        directory.listFiles { file -> file.name.startsWith(PENDING_PREFIX) }?.forEach { it.delete() }
    }

    private fun prefix(mode: RecordingMode) = when (mode) {
        RecordingMode.AUDIO -> "voice"
        RecordingMode.VIDEO -> "video"
    }

    private fun extension(mode: RecordingMode) = when (mode) {
        RecordingMode.AUDIO -> "m4a"
        RecordingMode.VIDEO -> "mp4"
    }

    private companion object {
        const val PENDING_PREFIX = "pending-"
    }
}
