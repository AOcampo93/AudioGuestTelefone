package com.olinix.audioguest.ui.saved

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens Android's share sheet (Bluetooth, Quick Share, e-mail…) for a saved message.
 * The chosen app gets read access to this one file only, through the app's file provider.
 */
fun shareMessage(context: Context, file: File, subject: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension == "mp4") "video/mp4" else "audio/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        // The clip carries the read grant on to whichever app the guest picks.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Started in the app's own task: while the app is pinned, Android blocks new tasks.
    context.startActivity(Intent.createChooser(send, title))
}
