package com.olinix.audioguest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao {

    /** Newest first, each with its number of messages. Emits again after every change to either table. */
    @Query(
        "SELECT events.*, (SELECT COUNT(*) FROM recordings WHERE recordings.eventId = events.id) AS messageCount " +
            "FROM events ORDER BY createdAt DESC, id DESC"
    )
    abstract fun observeSummaries(): Flow<List<EventSummary>>

    @Query("SELECT * FROM events WHERE id = :id")
    abstract suspend fun get(id: Long): Event?

    @Query("SELECT * FROM events ORDER BY createdAt DESC, id DESC LIMIT 1")
    abstract suspend fun newest(): Event?

    @Insert
    abstract suspend fun insert(event: Event): Long

    @Update
    abstract suspend fun update(event: Event)

    /** The event and the rows of its messages, together or not at all. Its files are deleted separately. */
    @Transaction
    open suspend fun deleteWithMessages(id: Long) {
        deleteMessages(id)
        delete(id)
    }

    @Query("DELETE FROM recordings WHERE eventId = :eventId")
    protected abstract suspend fun deleteMessages(eventId: Long)

    @Query("DELETE FROM events WHERE id = :id")
    protected abstract suspend fun delete(id: Long)
}
