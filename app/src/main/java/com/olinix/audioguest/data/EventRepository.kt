package com.olinix.audioguest.data

import com.olinix.audioguest.audio.EventStorage
import com.olinix.audioguest.settings.DeviceStore
import com.olinix.audioguest.settings.HostSettings
import kotlinx.coroutines.flow.Flow

/** The host's events. Exactly one is active: the booth records into it. Tests swap the database for a list. */
interface EventRepository {

    /** Every event, newest first, with how many messages each holds. */
    val summaries: Flow<List<EventSummary>>

    /** The event the booth works for, creating the first one if there is none yet. */
    suspend fun active(): Event

    suspend fun get(id: Long): Event?

    suspend fun activate(id: Long)

    suspend fun create(settings: HostSettings): Event

    /** Stores a change made in the panel. */
    suspend fun save(event: Event)

    /** Forgets the event and its messages. Their files are deleted separately. */
    suspend fun delete(id: Long)
}

class LocalEventRepository(
    private val dao: EventDao,
    private val device: DeviceStore,
    private val storage: EventStorage,
    private val now: () -> Long = System::currentTimeMillis,
) : EventRepository {

    override val summaries: Flow<List<EventSummary>> = dao.observeSummaries()

    override suspend fun active(): Event {
        device.activeEventId()?.let { id -> dao.get(id) }?.let { return it }
        val event = dao.newest() ?: firstEvent()
        device.setActiveEventId(event.id)
        return event
    }

    override suspend fun get(id: Long): Event? = dao.get(id)

    override suspend fun activate(id: Long) = device.setActiveEventId(id)

    override suspend fun create(settings: HostSettings): Event {
        val event = Event(createdAt = now(), settings = settings)
        return event.copy(id = dao.insert(event))
    }

    override suspend fun save(event: Event) = dao.update(event)

    override suspend fun delete(id: Long) = dao.deleteWithMessages(id)

    /**
     * Before events existed, the panel's settings, the messages and the host's videos belonged to
     * no event. They become the first one, whose id the database migration already gave the messages.
     */
    private suspend fun firstEvent(): Event {
        val event = Event(id = FIRST_EVENT_ID, createdAt = now(), settings = device.legacySettings() ?: HostSettings())
        dao.insert(event)
        storage.adoptLegacyFiles(FIRST_EVENT_ID)
        return event
    }

    private companion object {
        const val FIRST_EVENT_ID = 1L
    }
}
