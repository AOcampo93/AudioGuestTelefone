package com.olinix.audioguest.session

import com.olinix.audioguest.data.Recording

/** The gallery tabs: Todos · Voz · Video. */
enum class GalleryFilter {
    ALL,
    VOICE,
    VIDEO;

    fun accepts(recording: Recording): Boolean = when (this) {
        ALL -> true
        VOICE -> recording.mode == RecordingMode.AUDIO
        VIDEO -> recording.mode == RecordingMode.VIDEO
    }

    /** The messages under this tab, keeping the gallery order. */
    fun pick(recordings: List<Recording>): List<Recording> = recordings.filter(::accepts)
}
