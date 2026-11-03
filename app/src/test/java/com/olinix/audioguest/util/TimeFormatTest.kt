package com.olinix.audioguest.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class TimeFormatTest {

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:42", formatDuration(42))
        assertEquals("3:00", formatDuration(180))
    }

    @Test
    fun negativeSecondsShowAsZero() {
        assertEquals("0:00", formatDuration(-3))
    }

    @Test
    fun clockTimeUsesTwentyFourHours() {
        // 14 June 2027, 21:42 UTC.
        assertEquals("21:42", formatClockTime(1_813_009_320_000, ZoneId.of("UTC")))
        assertEquals("09:05", formatClockTime(1_812_963_900_000, ZoneId.of("UTC")))
    }

    @Test
    fun thePanelListsTheDayInTheScreenLanguage() {
        assertEquals("14 jun · 21:42", formatDayAndTime(1_813_009_320_000, Locale.forLanguageTag("es"), ZoneId.of("UTC")))
        assertEquals("14 jun · 21:42", formatDayAndTime(1_813_009_320_000, Locale.forLanguageTag("pt-PT"), ZoneId.of("UTC")))
        assertEquals("14 Jun · 21:42", formatDayAndTime(1_813_009_320_000, Locale.ENGLISH, ZoneId.of("UTC")))
    }
}
