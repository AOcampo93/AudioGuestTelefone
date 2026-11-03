package com.olinix.audioguest.data

import com.olinix.audioguest.session.RecordingMode
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Saved messages. The session talks to this interface, so tests can swap the database for a list. */
interface RecordingRepository {

    /** Every saved message, newest first. */
    val recordings: Flow<List<Recording>>

    /** Registers a file that was just kept. */
    suspend fun add(mode: RecordingMode, file: File, durationSeconds: Int)

    /** Forgets a message. Its file is deleted separately. */
    suspend fun delete(recording: Recording)
}

class LocalRecordingRepository(
    private val dao: RecordingDao,
    private val now: () -> Long = System::currentTimeMillis,
) : RecordingRepository {

    override val recordings: Flow<List<Recording>> = dao.observeAll()

    override suspend fun add(mode: RecordingMode, file: File, durationSeconds: Int) {
        dao.insert(Recording(mode = mode, fileName = file.name, durationSeconds = durationSeconds, savedAt = now()))
    }

    override suspend fun delete(recording: Recording) {
        dao.delete(recording)
    }
}
