package com.olinix.audioguest

import android.Manifest
import android.app.ActivityManager
import android.app.LocaleManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.olinix.audioguest.audio.Handset
import com.olinix.audioguest.audio.HandsetAudioRecorder
import com.olinix.audioguest.data.LocalRecordingRepository
import com.olinix.audioguest.lights.NoOpLightController
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.session.SessionViewModel
import com.olinix.audioguest.ui.AudioGuestApp
import com.olinix.audioguest.ui.theme.AudioGuestTheme
import com.olinix.audioguest.ui.theme.color
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val config by lazy {
        // For testing without a handset, as on the emulator: `adb shell am start -n com.olinix.audioguest/.MainActivity --ez require_handset false`
        SessionConfig(requireHandset = intent.getBooleanExtra(EXTRA_REQUIRE_HANDSET, true))
    }

    /** Pins the app so guests cannot leave it. Turned off only while developing: `--ez kiosk false`. */
    private val kiosk by lazy { intent.getBooleanExtra(EXTRA_KIOSK, true) }

    /** Set once the host chose "Salir de la app", so the app does not pin itself again on the way out. */
    private var leaving = false

    private val session: SessionViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as AudioGuestApplication
                SessionViewModel(
                    // MVP without an LED strip. Switching to WLED only changes this line.
                    lights = NoOpLightController(),
                    voiceRecorder = HandsetAudioRecorder(applicationContext),
                    videoRecorder = app.camera,
                    storage = app.storage,
                    repository = LocalRecordingRepository(app.database.recordings()),
                    events = app.events,
                    deliveries = app.deliveries,
                    handsetConnected = Handset.connection(applicationContext),
                    config = config,
                )
            }
        }
    }

    // The host grants this once while setting up the tablet, so guests never see the dialog.
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android briefly shows the status bar when the camera or microphone turns on. The app is
        // always dark, so its clock and icons must be light even when the tablet uses light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        askForMissingPermissions()
        followGuestLanguage()

        val camera = (application as AudioGuestApplication).camera
        setContent {
            val settings by session.settings.collectAsStateWithLifecycle()
            AudioGuestTheme(accent = settings.accent.color) {
                AudioGuestApp(
                    viewModel = session,
                    cameraPreview = camera.preview,
                    config = config,
                    onExitApp = ::exitKiosk,
                )
            }
        }
    }

    /** Guests see the app in the language the host picked. Android remembers it per app and restarts the screen. */
    private fun followGuestLanguage() {
        lifecycleScope.launch {
            session.settingsLoaded.first { it }
            session.settings.map { it.language }.distinctUntilChanged().collect { language ->
                val localeManager = getSystemService(LocaleManager::class.java)
                val wanted = LocaleList.forLanguageTags(language.tag)
                if (localeManager.applicationLocales != wanted) localeManager.applicationLocales = wanted
            }
        }
    }

    override fun onResume() {
        super.onResume()
        keepPinned()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Logged so the first test with the handset shows which key its button sends.
        if (event.device?.isExternal == true) Log.i(TAG, "Key ${KeyEvent.keyCodeToString(keyCode)} from ${event.device.name}")
        if (keyCode in HANDSET_BUTTONS) {
            if (event.repeatCount == 0) session.onHandsetButton()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Consumed on release too, or Android would pass the button on to a music app.
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        keyCode in HANDSET_BUTTONS || super.onKeyUp(keyCode, event)

    private fun askForMissingPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    /**
     * Screen pinning: no home, recents or notifications until the host leaves from the panel.
     * Android asks the host to confirm each time it pins. If someone unpins with the system
     * gesture, the tablet locks (as long as it has a screen lock) and the app asks again on return.
     */
    private fun keepPinned() {
        if (!kiosk || leaving || isPinned()) return
        Log.i(KIOSK_TAG, "Asking to pin the app")
        startLockTask()
    }

    private fun isPinned(): Boolean =
        getSystemService(ActivityManager::class.java).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    /** "Salir de la app" in the host panel. */
    private fun exitKiosk() {
        leaving = true
        if (isPinned()) stopLockTask()
        Log.i(KIOSK_TAG, "The host closed the app")
        finishAndRemoveTask()
    }

    /** Full screen with no status or navigation bar, like the kiosk. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        const val TAG = "Handset"
        const val KIOSK_TAG = "Kiosk"
        const val EXTRA_REQUIRE_HANDSET = "require_handset"
        const val EXTRA_KIOSK = "kiosk"
        val REQUIRED_PERMISSIONS = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

        /** Keys a handset's answer button may send: the classic headset hook, or play/pause. */
        val HANDSET_BUTTONS = setOf(KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY)
    }
}
