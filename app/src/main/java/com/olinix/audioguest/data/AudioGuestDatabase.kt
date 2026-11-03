package com.olinix.audioguest.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room works out each migration from the saved schemas. Version 2 added events: the events table,
 * and an eventId on every message that is 1 for those recorded before, the first event.
 * Version 3 added the guests' requests to receive their message.
 */
@Database(
    entities = [Recording::class, Event::class, Delivery::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class AudioGuestDatabase : RoomDatabase() {

    abstract fun recordings(): RecordingDao

    abstract fun events(): EventDao

    abstract fun deliveries(): DeliveryDao

    companion object {
        /** Opens the database file. The app keeps a single instance in [com.olinix.audioguest.AudioGuestApplication]. */
        fun open(context: Context): AudioGuestDatabase =
            Room.databaseBuilder(context, AudioGuestDatabase::class.java, "audioguest.db").build()
    }
}
