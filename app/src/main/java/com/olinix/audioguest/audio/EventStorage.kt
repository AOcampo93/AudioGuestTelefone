package com.olinix.audioguest.audio

import java.io.File

/** The tablet's folders: one per event, holding its messages and the host's videos, so events never mix. */
class EventStorage(private val root: File) {

    fun filesFor(eventId: Long): RecordingFiles = RecordingFiles(folder(eventId))

    /** Removes an event's messages and videos from the tablet. */
    fun delete(eventId: Long) {
        folder(eventId).deleteRecursively()
    }

    /**
     * Moves what was recorded before events existed, which lived straight in [root], into the
     * folder of [eventId]. Runs before that folder is first used; does nothing on later starts.
     */
    fun adoptLegacyFiles(eventId: Long) {
        val target = folder(eventId).apply { mkdirs() }
        for (name in LEGACY_NAMES) {
            val old = File(root, name)
            if (old.exists() && !old.renameTo(File(target, name))) {
                old.copyRecursively(File(target, name), overwrite = true)
                old.deleteRecursively()
            }
        }
    }

    private fun folder(eventId: Long) = File(root, "events/$eventId")

    private companion object {
        val LEGACY_NAMES = listOf("recordings", "welcome.mp4", "call.mp4")
    }
}
