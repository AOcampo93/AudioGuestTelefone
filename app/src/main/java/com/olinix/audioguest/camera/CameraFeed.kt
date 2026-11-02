package com.olinix.audioguest.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olinix.audioguest.ui.components.CameraBackdrop
import kotlinx.coroutines.flow.StateFlow

/** Live front-camera image filling the screen, with a dark placeholder while the camera opens. */
@Composable
fun CameraFeed(preview: StateFlow<SurfaceRequest?>, modifier: Modifier = Modifier) {
    val request by preview.collectAsStateWithLifecycle()
    Box(modifier) {
        CameraBackdrop()
        request?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}
