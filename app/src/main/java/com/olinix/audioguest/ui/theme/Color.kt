package com.olinix.audioguest.ui.theme

import androidx.compose.ui.graphics.Color
import com.olinix.audioguest.settings.Accent

// Mockup palette (EstilosApp): warm charcoal background and gold accent.
val Charcoal = Color(0xFF17120E)
val CharcoalWarm = Color(0xFF22190F)
val Surface = Color(0xFF1A140F)
val SurfaceDeep = Color(0xFF110D0A)
val SurfaceRaised = Color(0xFF241B13)

val Ivory = Color(0xFFF3EADF)
val IvorySoft = Color(0xFFE9DFD2)
val Taupe = Color(0xFFCDBFAE)
val Sand = Color(0xFFB9AA98)

/** Default accent. Hosts will be able to change it per event: see [AudioGuestTheme]. */
val Gold = Color(0xFFD6AB62)

/** Timer and progress bar during the last 10 seconds of a recording. */
val Amber = Color(0xFFFFA22E)
val AmberLight = Color(0xFFFFB44F)
val AmberMuted = Color(0xFFD9A35E)

/** Reserved for the "Recording" indicator and destructive actions. */
val RecordRed = Color(0xFFE5483D)
val RecordRedLight = Color(0xFFFF8178)

/** Errors and the "connect the handset" warning. */
val WarningOrange = Color(0xFFFF8A3D)

/** Translucent dark chip over the camera image. */
val Scrim = Color(0xC717120E)

/** Darkens the screen behind a dialog. */
val DialogScrim = Color(0xC70C0907)

// Host panel
val FieldBackground = Color(0xFF0F0B08)
val TabTrack = Color(0xFF1D1610)
val TabSelected = Color(0xFF2A2017)
val Muted = Color(0xFF9F8F7F)
val SavedGreen = Color(0xFF9FC7A8)

/** The host's accent as a Compose color. */
val Accent.color: Color get() = Color(argb)
