package com.olinix.audioguest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    /** Newest first, the order the gallery shows. Emits again after every insert. */
    @Query("SELECT * FROM recordings ORDER BY savedAt DESC, id DESC")
    fun observeAll(): Flow<List<Recording>>

    @Insert
    suspend fun insert(recording: Recording): Long

    @Delete
    suspend fun delete(recording: Recording)
}
