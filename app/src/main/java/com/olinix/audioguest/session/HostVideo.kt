package com.olinix.audioguest.session

import java.io.File

/** The host's two videos: the welcome before the menu, and the question of call mode. */
enum class HostVideo(val section: HostSection) {
    WELCOME(HostSection.WELCOME),
    CALL(HostSection.CALL),
}

/**
 * One of the host's videos on the tablet. [version] changes each time the file is replaced,
 * so screens reload its frame even though the path stays the same.
 */
data class HostVideoFile(val file: File, val version: Int)

/** Why a video the host picked was refused. */
enum class VideoProblem { TOO_LONG, UNREADABLE }
