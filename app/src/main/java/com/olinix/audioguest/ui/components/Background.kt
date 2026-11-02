package com.olinix.audioguest.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.olinix.audioguest.ui.theme.Charcoal
import com.olinix.audioguest.ui.theme.CharcoalWarm

/** Warm glow from the top edge that fades into charcoal, shared by every screen. */
fun Modifier.screenBackground(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            0f to CharcoalWarm,
            0.62f to Charcoal,
            center = Offset(size.width / 2, 0f),
            radius = size.height * 0.7f,
        )
    )
}
