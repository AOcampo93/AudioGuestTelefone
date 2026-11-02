package com.olinix.audioguest.util

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
