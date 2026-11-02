# Overview

AudioGuestTelefone is an event guestbook for Android tablets. Guests pick up a retro telephone handset connected over USB-C, choose on the screen whether to leave a voice or a video message, record it after a 3-2-1 countdown, review it, and save it. Hosts later get every message from the event.

The goal of this project is to learn native Android development with Kotlin and Jetpack Compose on real hardware: routing audio to and from a USB audio device, recording video with the front camera, storing recordings locally, and modeling the whole guest flow as a single state machine that drives both the screen and the event lights.

[Software Demo Video](http://youtube.link.goes.here)

# Development Environment

- Android Studio Rabbit 2 (2026.2.2) on macOS (Apple Silicon)
- Kotlin 2.4 with Jetpack Compose and Material 3
- Android Gradle Plugin 9.5, Gradle 9.6
- Target device: Lenovo Tab M11 (Android 13), portrait, kiosk-style full screen
- Hardware: retro USB-C telephone handset (microphone + speaker) through a USB-C hub

# Useful Websites

- [Kotlin documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Audio device routing (AudioManager)](https://developer.android.com/reference/android/media/AudioManager)
- [WLED JSON API](https://kno.wled.ge/interfaces/json-api/)

# Future Work

- LED strip control through a WLED controller (`WledLightController`)
- Background upload to a server and a QR code / email link for each guest
- Kiosk mode (Device Owner + Lock Task) and a PIN-protected host settings screen
- Portuguese and English UI translations

Fonts: Cormorant Garamond and DM Sans, licensed under the SIL Open Font License (see `licenses/`).
