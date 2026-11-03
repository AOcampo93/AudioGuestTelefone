package com.olinix.audioguest.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.olinix.audioguest.session.RecordingMode

/**
 * A message a guest saved. The audio or video itself is a file in the recordings folder;
 * this row is what the gallery lists.
 */
@Entity(tableName = "recordings", indices = [Index("eventId")])
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The event it was recorded at. Messages from before events existed belong to the first one. */
    @ColumnInfo(defaultValue = "1") val eventId: Long,
    val mode: RecordingMode,
    /** Name inside the recordings folder, not a full path, so the row survives a folder move. */
    val fileName: String,
    val durationSeconds: Int,
    /** When it was saved, in epoch milliseconds. */
    val savedAt: Long,
)
