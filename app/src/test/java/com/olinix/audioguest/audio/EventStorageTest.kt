package com.olinix.audioguest.audio

import com.olinix.audioguest.session.RecordingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EventStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun whatWasRecordedBeforeEventsMovesIntoTheFirstEvent() {
        val root = folder.root
        File(root, "recordings").mkdirs()
        File(root, "recordings/voice-1.m4a").writeText("voice")
        File(root, "welcome.mp4").writeText("welcome")

        val storage = EventStorage(root)
        storage.adoptLegacyFiles(1)

        assertEquals("voice", File(root, "events/1/recordings/voice-1.m4a").readText())
        assertEquals("welcome", File(root, "events/1/welcome.mp4").readText())
        assertFalse(File(root, "recordings").exists())
        assertFalse(File(root, "welcome.mp4").exists())

        // A second start finds nothing left to move.
        storage.adoptLegacyFiles(1)
        assertTrue(File(root, "events/1/recordings/voice-1.m4a").exists())
    }

    @Test
    fun deletingAnEventLeavesTheOthers() {
        val storage = EventStorage(folder.root)
        storage.filesFor(1).newPending(RecordingMode.AUDIO).writeText("a")
        storage.filesFor(2).newPending(RecordingMode.AUDIO).writeText("b")

        storage.delete(1)
        assertFalse(File(folder.root, "events/1").exists())
        assertTrue(File(folder.root, "events/2/recordings").listFiles()!!.isNotEmpty())
    }
}
