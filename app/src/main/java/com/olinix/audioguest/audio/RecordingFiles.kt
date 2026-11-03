package com.olinix.audioguest.audio

import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.HostVideo
import com.olinix.audioguest.session.RecordingMode
import java.io.File

/**
 * Where recordings live on the tablet. A new recording is "pending" until the guest presses
 * Guardar; pending files are deleted on retry, discard or when the guest walks away.
 */
class RecordingFiles(root: File) {

    private val directory = File(root, "recordings").apply { mkdirs() }

    /** The host's welcome and call videos. There is only ever one of each, kept apart from the guests' messages. */
    private val hostVideos = mapOf(
        HostVideo.WELCOME to File(root, "welcome.mp4"),
        HostVideo.CALL to File(root, "call.mp4"),
    )

    fun hostVideo(kind: HostVideo): File = hostVideos.getValue(kind)

    fun newPending(mode: RecordingMode): File =
        File(directory, "$PENDING_PREFIX${prefix(mode)}-${System.currentTimeMillis()}.${extension(mode)}")

    /** Turns a pending file into a kept one and returns its final location. */
    fun keep(pending: File): File {
        val kept = File(directory, pending.name.removePrefix(PENDING_PREFIX))
        if (!pending.renameTo(kept)) pending.copyTo(kept, overwrite = true).also { pending.delete() }
        return kept
    }

    /** Makes a just-recorded or just-copied video one of the host's videos, replacing the previous one. */
    fun keepAsHostVideo(pending: File, kind: HostVideo) {
        val target = hostVideo(kind)
        if (!pending.renameTo(target)) pending.copyTo(target, overwrite = true).also { pending.delete() }
    }

    fun deleteHostVideo(kind: HostVideo) {
        hostVideo(kind).delete()
    }

    fun fileFor(recording: Recording): File = File(directory, recording.fileName)

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
