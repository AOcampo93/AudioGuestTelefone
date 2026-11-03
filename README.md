# Overview

AudioGuestTelefone is an event guestbook for Android tablets. Guests pick up a retro telephone handset connected over USB-C, choose on the screen whether to leave a voice or a video message, record it after a 3-2-1 countdown, review it, and save it. Hosts later get every message from the event.

The goal of this project is to learn native Android development with Kotlin and Jetpack Compose on real hardware: routing audio to and from a USB audio device, recording video with the front camera, storing recordings locally, and modeling the whole guest flow as a single state machine that drives both the screen and the event lights.

[Software Demo Video](http://youtube.link.goes.here)

# Development Environment

- Android Studio Rabbit 2 (2026.2.2) on macOS (Apple Silicon)
- Kotlin 2.4 with Jetpack Compose and Material 3
- Android Gradle Plugin 9.5, Gradle 9.6
- Target device: Lenovo Tab M11 (tested on Android 15), portrait, kiosk-style full screen
- Hardware: retro USB-C telephone handset (microphone + speaker) through a USB-C hub

The app covers the screen with "Conecta el auricular" until the handset is plugged in. To try it without a handset (the emulator cannot plug one in), start it with:

```
adb shell am start -n com.olinix.audioguest/.MainActivity --ez require_handset false
```

Guests cannot leave the app: it pins itself with Android's screen pinning, which the host confirms once when the app opens. If someone unpins it with the system gesture, the tablet locks. The host opens the panel by holding the top-right corner of the start screen for 3 seconds and typing the PIN (set in `SessionConfig`); from there "Salir de la app" unpins and closes it. While developing, add `--ez kiosk false` to skip the pinning.

The panel has five sections, and every change is saved on the tablet at once (Jetpack DataStore):

- **Evento**: the event type (wedding, Christmas, birthday, company or other), the names it asks for, the date and the accent color, with a live preview of the start screen.
- **Bienvenida**: an optional welcome video of up to 30 seconds, recorded with the tablet or picked from its files, that plays before the menu through the handset or the tablet speaker.
- **Llamada**: call mode. Now and then, while nobody uses the booth, it rings like an old phone through the tablet speaker. Whoever answers (a tap or the handset button) hears the host's question video through the handset, and the answer is recorded right after it. Unanswered calls ring again later. Calls, including the test call, need the question video.
- **Invitados**: the language of the whole app, panel included (Portuguese by default, Spanish or English), which messages guests may leave (voice, video or both), whether they can see the saved messages and share their own, and show mode, which plays saved messages through the speaker while nobody uses the booth.
- **Mensajes**: every saved message, to play or delete. Guests cannot delete saved messages.

Guests choose the microphone (handset or tablet) in the menu and where they hear saved messages (handset or speaker) in the player. After saving, "Compartir mi mensaje" opens Android's share sheet (Bluetooth, Quick Share, e-mail) with that one file.

# Useful Websites

- [Kotlin documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Audio device routing (AudioManager)](https://developer.android.com/reference/android/media/AudioManager)
- [Room persistence library](https://developer.android.com/training/data-storage/room)
- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- [WLED JSON API](https://kno.wled.ge/interfaces/json-api/)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages)
- [Lock task mode and screen pinning](https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode)
- [Sharing files with FileProvider](https://developer.android.com/training/secure-file-sharing/setup-sharing)
- [AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)

# Future Work

- LED strip control through a WLED controller (`WledLightController`)
- Background upload to a server and a QR code / email link for each guest
- Full kiosk mode as Device Owner on a tablet dedicated to events (starts on boot, no unpin gesture)
- Changing the host PIN from the panel

Fonts: Cormorant Garamond and DM Sans, licensed under the SIL Open Font License (see `licenses/`).
