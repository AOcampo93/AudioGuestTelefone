package com.olinix.audioguest.export

import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.settings.HostSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Where exports go, inside the tablet's Download folder. */
const val EXPORT_ROOT = "AudioGuestTelefone"

/** The list of who asked for which message, next to the files. */
const val DELIVERY_LIST_NAME = "envios.csv"

/** "2026-12-25 Ana & Miguel": the event's own folder, by date first so events line up in order. */
fun exportFolderName(settings: HostSettings): String = safeFileName("${settings.date} ${settings.hosts}")

/** "2026-12-25 21.42.03 Voz.m4a": readable on any computer, in the order they were recorded. */
fun exportFileName(recording: Recording, kind: String, zone: ZoneId = ZoneId.systemDefault()): String {
    val time = Instant.ofEpochMilli(recording.savedAt).atZone(zone).format(FILE_TIME)
    val extension = recording.fileName.substringAfterLast('.', "")
    return safeFileName("$time $kind") + if (extension.isEmpty()) "" else ".$extension"
}

/** Leaves out the characters Windows and macOS refuse in file names. */
fun safeFileName(name: String): String = name.replace(FORBIDDEN, "").replace(Regex("""\s+"""), " ").trim()

/**
 * One CSV cell. Semicolons separate cells, as Excel expects in Portugal and Spain, so cells with
 * one are quoted. What a guest typed is kept from running as a formula: a leading = + - or @
 * gets an apostrophe, which Excel hides (a WhatsApp number starts with +).
 */
fun csvCell(value: String): String {
    val safe = if (value.firstOrNull() in FORMULA_STARTS) "'$value" else value
    return if (safe.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}

/** The whole file: a byte-order mark so Excel reads accents right, the header, then one line per row. */
fun csvText(header: String, rows: List<List<String>>): String =
    "\uFEFF" + (listOf(header) + rows.map { row -> row.joinToString(";", transform = ::csvCell) }).joinToString("\r\n") + "\r\n"

private val FORBIDDEN = Regex("""[\\/:*?"<>|]""")
private val FORMULA_STARTS = setOf('=', '+', '-', '@')
private val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss")
