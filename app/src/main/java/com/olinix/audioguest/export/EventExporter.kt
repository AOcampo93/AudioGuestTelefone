package com.olinix.audioguest.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.olinix.audioguest.R
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.data.Delivery
import com.olinix.audioguest.data.DeliveryChannel
import com.olinix.audioguest.data.Event
import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What an export left in the Download folder. */
data class ExportResult(
    /** "Download/AudioGuestTelefone/2026-12-25 Ana & Miguel", as the host finds it on a computer. */
    val folder: String,
    /** Messages in the folder now. */
    val total: Int,
    /** Of those, the ones copied this time; the rest were already there from an earlier export. */
    val copied: Int,
    /** Every exported file, envios.csv included, as content URIs to share. */
    val uris: List<String>,
)

/** Copies an event out of the app's private storage, where only the host can then take it. */
interface EventExporter {

    /** Throws [IOException] if a file could not be written, for instance with the storage full. */
    suspend fun export(event: Event, recordings: List<Recording>, deliveries: List<Delivery>, files: RecordingFiles): ExportResult
}

/**
 * Exports into the tablet's shared Download folder through MediaStore, which needs no permission.
 * From there the host takes the files by USB cable, or shares them. Files already exported are
 * skipped, so exporting again after the event only copies the new ones; envios.csv is rewritten.
 */
class DownloadsExporter(private val context: Context) : EventExporter {

    private val resolver = context.contentResolver
    private val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

    override suspend fun export(
        event: Event,
        recordings: List<Recording>,
        deliveries: List<Delivery>,
        files: RecordingFiles,
    ): ExportResult = withContext(Dispatchers.IO) {
        val folder = "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_ROOT/${exportFolderName(event.settings)}"
        val existing = existingFiles("$folder/")
        val uris = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        var copied = 0
        for (recording in recordings.sortedBy { it.savedAt }) {
            val name = exportFileName(recording, kindOf(recording))
            val uri = existing[name] ?: write(name, mimeOf(recording), folder) { output ->
                files.fileFor(recording).inputStream().use { it.copyTo(output) }
            }.also { copied++ }
            uris += uri.toString()
            rows += row(name, recording, deliveries.firstOrNull { it.recordingId == recording.id })
        }
        val list = csvText(context.getString(R.string.export_csv_header), rows).toByteArray()
        uris += write(DELIVERY_LIST_NAME, "text/csv", folder, existing[DELIVERY_LIST_NAME]) { it.write(list) }.toString()
        ExportResult(folder = folder, total = recordings.size, copied = copied, uris = uris)
    }

    /** The files this app already put in [relativePath], by name. Files of other apps are not visible, nor needed. */
    private fun existingFiles(relativePath: String): Map<String, Uri> {
        val found = mutableMapOf<String, Uri>()
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                found[cursor.getString(1)] = Uri.withAppendedPath(collection, cursor.getLong(0).toString())
            }
        }
        return found
    }

    /**
     * Writes a file into the Download folder, or over [replace]. It stays hidden from other apps
     * (and from a computer on the cable) until it is complete.
     */
    private fun write(name: String, mime: String, folder: String, replace: Uri? = null, content: (OutputStream) -> Unit): Uri {
        if (replace != null) {
            resolver.openOutputStream(replace, "wt")?.use(content) ?: throw IOException("Cannot rewrite $name")
            return replace
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Cannot create $name")
        try {
            resolver.openOutputStream(uri)?.use(content) ?: throw IOException("Cannot write $name")
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return uri
    }

    /** file; type; length; recorded; channel; contact; news; sent. */
    private fun row(name: String, recording: Recording, delivery: Delivery?): List<String> {
        val recorded = Instant.ofEpochMilli(recording.savedAt).atZone(ZoneId.systemDefault()).format(ROW_TIME)
        if (delivery == null) return listOf(name, kindOf(recording), formatDuration(recording.durationSeconds), recorded, "", "", "", "")
        val sent = delivery.sentAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(ROW_TIME) } ?: answer(false)
        return listOf(
            name,
            kindOf(recording),
            formatDuration(recording.durationSeconds),
            recorded,
            context.getString(if (delivery.channel == DeliveryChannel.EMAIL) R.string.delivery_email else R.string.delivery_whatsapp),
            delivery.address,
            answer(delivery.wantsNews),
            sent,
        )
    }

    private fun answer(yes: Boolean) = context.getString(if (yes) R.string.export_yes else R.string.export_no)

    private fun kindOf(recording: Recording) =
        context.getString(if (recording.mode == RecordingMode.VIDEO) R.string.export_kind_video else R.string.export_kind_voice)

    private fun mimeOf(recording: Recording) = if (recording.mode == RecordingMode.VIDEO) "video/mp4" else "audio/mp4"

    private companion object {
        val ROW_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
