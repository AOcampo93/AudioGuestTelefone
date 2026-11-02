package com.olinix.audioguest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Recording::class], version = 1)
abstract class AudioGuestDatabase : RoomDatabase() {

    abstract fun recordings(): RecordingDao

    companion object {
        @Volatile
        private var instance: AudioGuestDatabase? = null

        /** One database for the whole app, opened on first use. */
        fun get(context: Context): AudioGuestDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AudioGuestDatabase::class.java, "audioguest.db")
                    .build()
                    .also { instance = it }
            }
    }
}
