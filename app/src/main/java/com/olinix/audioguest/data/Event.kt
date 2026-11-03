package com.olinix.audioguest.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.olinix.audioguest.settings.HostSettings

/** One event the booth works at: its own setup in the panel, and the messages recorded during it. */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** When the host created it, in epoch milliseconds. */
    val createdAt: Long,
    @Embedded val settings: HostSettings,
)

/** An event as the panel lists it, with how many messages it holds. */
data class EventSummary(
    @Embedded val event: Event,
    val messageCount: Int,
)
