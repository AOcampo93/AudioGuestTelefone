package com.olinix.audioguest.lights

import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class LightSceneTest {

    @Test
    fun idleShowsInvitation() {
        assertEquals(LightScene.INVITATION, lightSceneFor(SessionState.Idle))
    }

    @Test
    fun videoRecordingUsesFillLight() {
        val state = SessionState.Capturing(RecordingMode.VIDEO, elapsedSeconds = 42, maxSeconds = 120)
        assertEquals(LightScene.RECORDING_VIDEO, lightSceneFor(state))
    }

    @Test
    fun lastTenSecondsPulseAmber() {
        val state = SessionState.Capturing(RecordingMode.AUDIO, elapsedSeconds = 170, maxSeconds = 180)
        assertEquals(LightScene.LAST_SECONDS, lightSceneFor(state))
    }
}
