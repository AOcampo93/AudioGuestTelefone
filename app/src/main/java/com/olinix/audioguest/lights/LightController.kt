package com.olinix.audioguest.lights

import android.util.Log

/** All the app needs to know about the lights. */
interface LightController {
    fun show(scene: LightScene)
}

/**
 * MVP version without hardware: it only logs which scene would play.
 * To drive the strip, write a WledLightController and change the line that creates it.
 */
class NoOpLightController : LightController {
    override fun show(scene: LightScene) {
        Log.d(TAG, "Light scene: $scene")
    }

    private companion object {
        const val TAG = "Lights"
    }
}
