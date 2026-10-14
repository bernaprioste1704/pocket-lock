# Installing Pocket Lock on your Android phone

About 5 minutes. Needs Android 8.0 or newer.

## 1. Download the APK on your phone

1. On your phone, open the browser.
2. Go to **https://github.com/bernaprioste1704/pocket-lock/releases/latest**
3. Under **Assets**, tap **`PocketLock-vX.Y.Z.apk`** to download it.
   - If Chrome warns *"This type of file can harm your device"*, tap **Download anyway**.

> Alternative from your Mac: download the APK there and send it to your phone (Google Drive, email to yourself, USB cable…), then open it on the phone.

## 2. Install it

1. Open the downloaded file (from the download notification, or **Files → Downloads**).
2. Android will say installing from this source isn't allowed. Tap **Settings** → turn on **Allow from this source** → go back.
3. Tap **Install**.
4. If **Google Play Protect** shows *"Unsafe app blocked"* or *"Unknown app"*, tap **More details → Install anyway**. This happens because the app isn't from the Play Store. Play Protect may also suggest a scan. Either choice is fine.

## 3. Allow "restricted settings" (Android 13 and newer)

Android blocks accessibility access for apps installed outside the Play Store until you allow it once. If you skip this, the Pocket Lock switch in step 4 is greyed out or says *"Restricted setting"*.

1. Open **Settings → Apps → Pocket Lock** (or long-press the Pocket Lock icon → **App info**).
2. Tap the **⋮** menu in the top-right corner → **Allow restricted settings**. Confirm with your PIN or fingerprint.
   - If you don't see this option, go straight to step 4. If the toggle there is blocked, tap it once (the "restricted" message counts as an attempt), then come back here and the option will appear.

## 4. Turn on Pocket Lock

1. Open the **Pocket Lock** app and tap **Enable Pocket Lock**.
2. Find **Pocket Lock** in the list. On Samsung it's under **Installed apps**, on others **Downloaded apps**.
3. Turn it **on** and accept the permission prompt.
   - Android shows a scary "full control of your device" warning for every accessibility app. Pocket Lock only uses it to draw the black screen and listen for the volume-button unlock. It has no internet permission, so it can't send anything anywhere.
4. Go back to the app. It should say **✅ Pocket Lock is enabled**.

## 5. Add the button

**Quick Settings tile (recommended):** in the app, tap **Add Quick Settings tile → Add tile**.
On Android 12 and older, swipe down twice from the top, tap the ✏️ pencil, and drag **Pocket Lock** into your tiles.

**Floating button (optional):** in **Settings → Accessibility → Pocket Lock**, turn on **Pocket Lock shortcut**. A small accessibility button appears at the edge of your screen, and tapping it blanks the screen.

## Using it

1. Start your video (YouTube, Instagram, anything).
2. Swipe down and tap the **Pocket Lock** tile. The screen goes black and the video keeps playing.
3. **To unlock:** hold **two fingers** on the screen for 1.5 seconds, **or** press **Volume Up + Volume Down** at the same time.

Good to know:
- Single volume presses still change the volume while it's black.
- Double-tap the black screen to show a reminder of how to unlock.
- Pressing the **power button** really locks the phone (Pocket Lock steps aside), and most video apps will pause.
- The screen stays technically on (at minimum brightness, all black), so your phone won't auto-lock while it's blanked. Unlock it when you're done.

## Updating

Download the newer APK from the releases page and install it over the old one. Settings are kept.

## Troubleshooting

| Problem | Fix |
|---|---|
| Pocket Lock toggle is greyed out / "Restricted setting" | Do step 3 |
| Tile says "Setup needed" | Accessibility access is off, so redo step 4. Some phones turn it off after the app is force-stopped or "optimized". |
| Samsung/Xiaomi keeps disabling it | Settings → Apps → Pocket Lock → Battery → **Unrestricted** |
| Edge swipes still go home/back | System navigation gestures can't be blocked by any app. Use the 3-button navigation, or just be aware of it. |
| "App not installed" when updating | The new APK was signed with a different key. Uninstall the old one first. |
