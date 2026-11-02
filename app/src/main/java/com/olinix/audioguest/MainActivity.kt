package com.olinix.audioguest

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.olinix.audioguest.lights.LightController
import com.olinix.audioguest.lights.NoOpLightController
import com.olinix.audioguest.lights.lightSceneFor
import com.olinix.audioguest.session.EventInfo
import com.olinix.audioguest.session.SessionState
import com.olinix.audioguest.ui.idle.IdleScreen
import com.olinix.audioguest.ui.theme.AudioGuestTheme

class MainActivity : ComponentActivity() {

    // MVP without an LED strip. Switching to WLED only changes this line.
    private val lights: LightController = NoOpLightController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lights.show(lightSceneFor(SessionState.Idle))

        setContent {
            AudioGuestTheme {
                IdleScreen(
                    event = EventInfo.Sample,
                    // Next step: a SessionViewModel that moves to SessionState.Menu.
                    onTap = {},
                )
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
