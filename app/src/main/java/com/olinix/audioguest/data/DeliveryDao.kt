package com.olinix.audioguest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    /** One event's requests, oldest first, the order they should be sent in. */
    @Query("SELECT * FROM deliveries WHERE eventId = :eventId ORDER BY createdAt, id")
    fun observe(eventId: Long): Flow<List<Delivery>>

    @Insert
    suspend fun insert(delivery: Delivery): Long

    @Query("UPDATE deliveries SET sentAt = :sentAt WHERE id = :id")
    suspend fun setSent(id: Long, sentAt: Long?)

    @Query("DELETE FROM deliveries WHERE recordingId = :recordingId")
    suspend fun deleteForRecording(recordingId: Long)
}
