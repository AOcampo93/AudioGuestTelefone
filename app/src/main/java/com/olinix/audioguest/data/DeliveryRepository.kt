package com.olinix.audioguest.data

import kotlinx.coroutines.flow.Flow

/** Who asked to receive which message. The session talks to this interface, so tests can keep them in a list. */
interface DeliveryRepository {

    fun deliveries(eventId: Long): Flow<List<Delivery>>

    suspend fun add(delivery: Delivery)

    /** Ticks a request off as sent, or back to "to send". */
    suspend fun setSent(id: Long, sent: Boolean)

    /** Forgets the requests for a message that was deleted. */
    suspend fun deleteForRecording(recordingId: Long)
}

class LocalDeliveryRepository(
    private val dao: DeliveryDao,
    private val now: () -> Long = System::currentTimeMillis,
) : DeliveryRepository {

    override fun deliveries(eventId: Long): Flow<List<Delivery>> = dao.observe(eventId)

    override suspend fun add(delivery: Delivery) {
        dao.insert(delivery)
    }

    override suspend fun setSent(id: Long, sent: Boolean) = dao.setSent(id, if (sent) now() else null)

    override suspend fun deleteForRecording(recordingId: Long) = dao.deleteForRecording(recordingId)
}
