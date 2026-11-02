package com.olinix.audioguest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Recording::class], version = 1)
abstract class AudioGuestDatabase : RoomDatabase() {

    abstract fun recordings(): RecordingDao

    companion object {
        /** Opens the database file. The app keeps a single instance in [com.olinix.audioguest.AudioGuestApplication]. */
        fun open(context: Context): AudioGuestDatabase =
            Room.databaseBuilder(context, AudioGuestDatabase::class.java, "audioguest.db").build()
    }
}
