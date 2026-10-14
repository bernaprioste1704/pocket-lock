# Pocket Lock

An Android app that **blacks out your screen and blocks touches, while the video underneath keeps playing**. Lock the screen, put the phone in your pocket, and keep listening.

## Why not just lock the phone?

When you really lock the phone, the video app gets sent to the background, and most apps (YouTube without Premium, Instagram, TikTok, …) pause when that happens. Pocket Lock never really locks the phone. It draws a black layer **on top** of the video app, so the app stays in the foreground and keeps playing. On OLED screens black pixels are off, so it uses very little battery.

## How to use it

| To… | Do this |
|---|---|
| **Blank the screen** | Tap the **Pocket Lock** Quick Settings tile, or the floating accessibility button |
| **Unlock** | Hold **two fingers** on the screen for 1.5 s, **or** press **Volume Up + Volume Down** together |
| See a reminder | Double-tap the black screen |
| Change volume | Volume buttons work normally while blanked |

👉 **Installing on your phone: see [INSTALL.md](INSTALL.md).**

## How it works

- `LockService` is an Android **AccessibilityService**. It's the only kind of app allowed to draw a window above everything (`TYPE_ACCESSIBILITY_OVERLAY`) and to watch the volume keys.
- The overlay is `FLAG_NOT_FOCUSABLE`, so the video app keeps window focus and stays `RESUMED`. It's also `FLAG_KEEP_SCREEN_ON` with the brightness set to minimum.
- `LockTileService` is the Quick Settings tile. It closes the notification shade, then blanks the screen.
- If you press the power button (a real lock), the overlay is removed automatically.

## Known limits

- The system home/back **gesture swipes from the screen edge** may still work on some phones. They're handled by Android before any app sees them.
- Android only. iOS doesn't allow apps to do this.

## Development

```bash
./gradlew testDebugUnitTest       # JVM unit tests (unlock logic)
./gradlew connectedDebugAndroidTest  # on a connected device/emulator
./gradlew assembleDebug
```

CI (`.github/workflows/ci.yml`) runs unit tests and the emulator tests on Android 10 and Android 14 on every push. Pushing a `v*` tag builds a signed APK and publishes it as a GitHub Release (`release.yml`).
