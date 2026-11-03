package com.olinix.audioguest

import android.app.Application
import com.olinix.audioguest.audio.EventStorage
import com.olinix.audioguest.camera.FrontCameraRecorder
import com.olinix.audioguest.data.AudioGuestDatabase
import com.olinix.audioguest.data.EventRepository
import com.olinix.audioguest.data.LocalEventRepository
import com.olinix.audioguest.settings.DeviceStore

/** Objects that live as long as the app, so every screen and the session share the same ones. */
class AudioGuestApplication : Application() {

    val database: AudioGuestDatabase by lazy { AudioGuestDatabase.open(this) }

    /** The session records with it and the screens show its preview. */
    val camera: FrontCameraRecorder by lazy { FrontCameraRecorder(this) }

    /** One folder per event for its messages and videos. */
    val storage: EventStorage by lazy { EventStorage(filesDir) }

    val events: EventRepository by lazy { LocalEventRepository(database.events(), DeviceStore(this), storage) }
}
