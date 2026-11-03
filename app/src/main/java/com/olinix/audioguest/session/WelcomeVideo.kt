package com.olinix.audioguest.session

import java.io.File

/**
 * The host's welcome video. [version] changes each time the file is replaced, so screens
 * reload its thumbnail even though the path stays the same.
 */
data class WelcomeVideo(val file: File, val version: Int)

/** Why a video the host chose from the tablet was not used. */
enum class WelcomeProblem { TOO_LONG, UNREADABLE }
