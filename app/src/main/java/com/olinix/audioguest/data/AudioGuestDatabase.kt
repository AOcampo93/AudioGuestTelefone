package com.olinix.audioguest.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 2 added events. Room works out the migration from the saved schemas: the new events
 * table, and an eventId on every message that is 1 for those recorded before, the first event.
 */
@Database(
    entities = [Recording::class, Event::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AudioGuestDatabase : RoomDatabase() {

    abstract fun recordings(): RecordingDao

    abstract fun events(): EventDao

    companion object {
        /** Opens the database file. The app keeps a single instance in [com.olinix.audioguest.AudioGuestApplication]. */
        fun open(context: Context): AudioGuestDatabase =
            Room.databaseBuilder(context, AudioGuestDatabase::class.java, "audioguest.db").build()
    }
}
