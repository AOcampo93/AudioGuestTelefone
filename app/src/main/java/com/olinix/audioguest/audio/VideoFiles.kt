package com.olinix.audioguest.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * A frame half a second into the video, read off the main thread. Null until it is ready or if it fails.
 * [version] reloads it when a file is replaced at the same path, as the welcome video is.
 */
@Composable
fun rememberVideoFrame(file: File, version: Any? = null): ImageBitmap? {
    val frame by produceState<ImageBitmap?>(initialValue = null, file, version) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(file.path)
                    retriever.getFrameAtTime(500_000)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return frame
}

/** Length of a video in whole seconds, or null if the file is not a video Android can read. */
fun videoSeconds(file: File): Int? = try {
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(file.path)
        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
        val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        if (hasVideo && millis != null) (millis / 1000).toInt() else null
    }
} catch (e: RuntimeException) {
    // setDataSource refuses files it cannot open or parse.
    null
}

/**
 * Copies a video the host picked from the tablet's files into [target], off the main thread,
 * and returns its length in seconds, or null if it could not be copied or read.
 */
suspend fun copyVideo(context: Context, uri: Uri, target: File): Int? = withContext(Dispatchers.IO) {
    try {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        videoSeconds(target)
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }
}
