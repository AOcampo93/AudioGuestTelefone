package com.olinix.audioguest

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olinix.audioguest.lights.NoOpLightController
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.SessionConfig
import com.olinix.audioguest.session.SessionViewModel
import com.olinix.audioguest.ui.AudioGuestApp
import com.olinix.audioguest.ui.theme.AudioGuestTheme

class MainActivity : ComponentActivity() {

    private val config = SessionConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AudioGuestTheme {
                val session = viewModel {
                    // MVP without an LED strip. Switching to WLED only changes this line.
                    SessionViewModel(lights = NoOpLightController(), config = config)
                }
                AudioGuestApp(viewModel = session, event = EventInfo.Sample, config = config)
            }
        }
    }

    /** Full screen with no status or navigation bar, like the kiosk. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
