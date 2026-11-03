package com.olinix.audioguest.settings

import com.olinix.audioguest.session.RecordingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HostSettingsTest {

    @Test
    fun aCallIsAnsweredOnlyWithAnAllowedKind() {
        assertEquals(RecordingMode.AUDIO, HostSettings(callAnswer = RecordingMode.AUDIO).callAnswerMode)
        assertEquals(RecordingMode.VIDEO, HostSettings(allowed = AllowedMessages.VIDEO_ONLY, callAnswer = RecordingMode.AUDIO).callAnswerMode)
        assertEquals(RecordingMode.AUDIO, HostSettings(allowed = AllowedMessages.VOICE_ONLY, callAnswer = RecordingMode.VIDEO).callAnswerMode)
    }

    @Test
    fun voiceOnlyLeavesVideoOut() {
        assertFalse(HostSettings(allowed = AllowedMessages.VOICE_ONLY).allows(RecordingMode.VIDEO))
    }
}
