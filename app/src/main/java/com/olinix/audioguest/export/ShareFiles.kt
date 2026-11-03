package com.olinix.audioguest.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens Android's share sheet (Quick Share, Drive, Bluetooth…) with every exported file at once.
 * Only the host reaches it, from the panel; guests never leave the app.
 */
fun shareFiles(context: Context, uris: List<String>, title: String) {
    val parsed = uris.map(Uri::parse)
    if (parsed.isEmpty()) return
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(parsed))
        // The clip carries the read grant on to whichever app the host picks.
        clipData = ClipData.newRawUri(null, parsed.first()).apply { parsed.drop(1).forEach { addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Started in the app's own task: while the app is pinned, Android blocks new tasks.
    context.startActivity(Intent.createChooser(send, title))
}
