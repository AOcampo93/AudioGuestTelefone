package com.olinix.audioguest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Single, always-dark theme: the app lives in dimly lit venues.
 * [accent] is swappable because each host will be able to pick their color;
 * screens read it from MaterialTheme.colorScheme.primary.
 */
@Composable
fun AudioGuestTheme(
    accent: Color = Gold,
    content: @Composable () -> Unit,
) {
    val colors = darkColorScheme(
        primary = accent,
        onPrimary = Charcoal,
        secondary = Taupe,
        onSecondary = Charcoal,
        tertiary = Amber,
        error = RecordRed,
        background = Charcoal,
        onBackground = Ivory,
        surface = Surface,
        onSurface = Ivory,
        surfaceVariant = SurfaceRaised,
        onSurfaceVariant = Sand,
    )

    MaterialTheme(
        colorScheme = colors,
        typography = AudioGuestTypography,
        content = content,
    )
}
