# Rafay CC — root-backed iOS-style Control Center

A clean control center overlay for rooted Android. No ads, no dead buttons.
Toggles work because they run through `su` — the thing third-party apps can't do.

- **Top-right edge swipe** opens it (top-left/center stays free for Samsung's shade)
- Wi-Fi · Mobile Data · Bluetooth · Airplane · Rotation · Flashlight + brightness slider
- Tap outside or press back to close

---

## Getting the APK (no Android Studio needed)

1. Make a new GitHub repo (e.g. `rafay-cc`) and push this whole folder to it.
   ```bash
   git init && git add . && git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/Hirafay/rafay-cc.git
   git push -u origin main
   ```
2. Go to the repo's **Actions** tab. The "Build APK" workflow runs automatically.
3. When it's green, open the run → **Artifacts** → download `RafayCC-debug-apk`.
   Unzip it → `app-debug.apk`.

### Or build locally
With Android Studio: open the folder, let it sync, Build → Build APK.
With CLI + Android SDK installed: `gradle assembleDebug` (APK lands in
`app/build/outputs/apk/debug/`).

---

## Install + first run

1. Sideload `app-debug.apk` (or `adb install app-debug.apk`).
2. Open the app. It checks root on launch — grant **su** to it in KernelSU when prompted.
3. Tap **Start Control Center**, grant **"Display over other apps"**, tap Start again.
4. Swipe down from the **top-right edge**.

Grant the overlay permission instantly via root instead of digging through menus:
```bash
su
appops set com.rafay.controlcenter SYSTEM_ALERT_WINDOW allow
```

---

## Tuning for ArtisanROM (the bulletproofing part)

The only commands that are ROM-dependent are **Bluetooth** and **Mobile Data**.
If a toggle does nothing, that's the command not matching your ROM — fix one line
in `Toggles.kt`:

- Bluetooth alt: `cmd bluetooth_manager enable` / `disable`
- Mobile data alt: `settings put global mobile_data 1` / `0`

Everything else (Wi-Fi, airplane, rotation, brightness, flashlight) is standard
and should work as-is on Android 16.

To restyle toward your iOS look: panel color + corner radius live in
`OverlayService.showPanel()`, the active-toggle blue (`#0A84FF`) is in `makeToggle()`.
