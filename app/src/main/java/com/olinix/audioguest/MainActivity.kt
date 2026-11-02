package com.olinix.audioguest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olinix.audioguest.audio.HandsetAudioRecorder
import com.olinix.audioguest.audio.RecordingFiles
import com.olinix.audioguest.data.AudioGuestDatabase
import com.olinix.audioguest.data.LocalRecordingRepository
import com.olinix.audioguest.lights.NoOpLightController
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.session.SessionViewModel
import com.olinix.audioguest.ui.AudioGuestApp
import com.olinix.audioguest.ui.theme.AudioGuestTheme

class MainActivity : ComponentActivity() {

    private val config = SessionConfig()

    // The host grants this once while setting up the tablet, so guests never see the dialog.
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        askForMissingPermissions()

        setContent {
            AudioGuestTheme {
                val session = viewModel {
                    SessionViewModel(
                        // MVP without an LED strip. Switching to WLED only changes this line.
                        lights = NoOpLightController(),
                        recorder = HandsetAudioRecorder(applicationContext),
                        files = RecordingFiles(filesDir),
                        repository = LocalRecordingRepository(AudioGuestDatabase.get(applicationContext).recordings()),
                        config = config,
                    )
                }
                AudioGuestApp(viewModel = session, event = EventInfo.Sample, config = config)
            }
        }
    }

    private fun askForMissingPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    /** Full screen with no status or navigation bar, like the kiosk. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        val REQUIRED_PERMISSIONS = listOf(Manifest.permission.RECORD_AUDIO)
    }
}
