package com.olinix.audioguest.audio

import kotlin.math.log10

/** Anything quieter than -50 dB counts as silence. */
private const val SILENCE_DB = 50.0

/**
 * Turns a peak amplitude (a fraction of full scale, 0..1) into a 0..1 loudness.
 * The scale is in decibels, like the ear, so normal speech moves the wave well above the floor.
 */
fun loudness(amplitude: Double): Float =
    if (amplitude <= 0) 0f else ((20 * log10(amplitude) + SILENCE_DB) / SILENCE_DB).toFloat().coerceIn(0f, 1f)

/** Rises at once but falls slowly, so the wave does not flicker between words. */
fun smoothLevel(previous: Float, current: Float): Float = maxOf(current, previous * 0.85f)
