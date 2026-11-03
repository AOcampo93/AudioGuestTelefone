package com.olinix.audioguest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    /** One event's messages, newest first, the order the gallery shows. Emits again after every change. */
    @Query("SELECT * FROM recordings WHERE eventId = :eventId ORDER BY savedAt DESC, id DESC")
    fun observe(eventId: Long): Flow<List<Recording>>

    @Insert
    suspend fun insert(recording: Recording): Long

    @Delete
    suspend fun delete(recording: Recording)
}
