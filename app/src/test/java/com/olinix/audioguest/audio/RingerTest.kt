package com.olinix.audioguest.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RingerTest {

    private val rate = 8_000
    private val cycle = ringCycle(rate)

    private fun loudest(from: Double, to: Double): Int =
        (((from * rate).toInt()) until (to * rate).toInt()).maxOf { abs(cycle[it].toInt()) }

    @Test
    fun oneCycleLastsThreeSeconds() {
        assertEquals(3 * rate, cycle.size)
    }

    @Test
    fun ringsTwiceThenRests() {
        assertTrue(loudest(0.05, 0.35) > 10_000)
        assertEquals(0, loudest(0.42, 0.58))
        assertTrue(loudest(0.65, 0.95) > 10_000)
        assertEquals(0, loudest(1.02, 3.0))
    }
}
