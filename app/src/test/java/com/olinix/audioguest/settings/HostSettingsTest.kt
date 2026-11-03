package com.olinix.audioguest.settings

import com.olinix.audioguest.session.RecordingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class HostSettingsTest {

    @Test
    fun aCallIsAnsweredOnlyWithAnAllowedKind() {
        assertEquals(RecordingMode.AUDIO, HostSettings(callAnswer = RecordingMode.AUDIO).callAnswerMode)
        assertEquals(RecordingMode.VIDEO, HostSettings(allowed = AllowedMessages.VIDEO_ONLY, callAnswer = RecordingMode.AUDIO).callAnswerMode)
        assertEquals(RecordingMode.AUDIO, HostSettings(allowed = AllowedMessages.VOICE_ONLY, callAnswer = RecordingMode.VIDEO).callAnswerMode)
    }

    @Test
    fun aNewEventKeepsHowTheBoothWorksButNotTheNamesOrVideos() {
        val party = HostSettings(
            type = EventType.BIRTHDAY,
            birthdayName = "Lucía",
            language = GuestLanguage.ENGLISH,
            allowed = AllowedMessages.VIDEO_ONLY,
            welcomeVideo = true,
            callMode = true,
            callEveryMinutes = 20,
        )
        val next = party.forNewEvent(LocalDate.of(2027, 1, 9))
        assertEquals(GuestLanguage.ENGLISH, next.language)
        assertEquals(AllowedMessages.VIDEO_ONLY, next.allowed)
        assertEquals(20, next.callEveryMinutes)
        assertEquals(LocalDate.of(2027, 1, 9), next.date)
        assertEquals(HostSettings().type, next.type)
        assertEquals(HostSettings().birthdayName, next.birthdayName)
        assertFalse(next.welcomeVideo)
        assertFalse(next.callMode)
    }

    @Test
    fun voiceOnlyLeavesVideoOut() {
        assertFalse(HostSettings(allowed = AllowedMessages.VOICE_ONLY).allows(RecordingMode.VIDEO))
    }
}
