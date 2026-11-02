# NOVA — a Liquid Glass launcher for Android TV

NOVA is a bespoke home screen for Android TV / Google TV (built for a TCL 50C6KS on Android TV OS 12).
It's written in Kotlin with Jetpack Compose, with a cinematic deep-space backdrop, translucent glass
surfaces, and spectral violet / blue / cyan / pink lighting.

## What it does

- **Favourites**: four hero cards for **Nuvio, YouTube, Netflix and Prime Video**, each with its own
  generative holographic artwork (orbits, waves, light curtains, horizon). Apps are matched by name and
  package keywords against what's installed; no package IDs are hard-coded. A card for an app that
  isn't installed says so, and pressing OK opens a store search for it.
- **Library**: every launchable app on the TV (TV apps plus phone-style apps), discovered at runtime,
  sorted A–Z and shown with its TV banner or icon. The list refreshes automatically when apps are
  installed or removed.
- **Your own favourites**: hold **OK** (or press **Menu**) on any app, then choose *Add to Favourites*.
  Pinned apps appear after the four hero cards.
- **Header**: NOVA wordmark, time (follows the TV's 12/24-hour setting), date, a ✦ button for Home
  setup and a ⚙ button for Settings.
- **Remote-first**: built for D-pad use. Focused items lift, glow in the app's own colour, show a light
  sheen and a rotating spectral rim, and tint the ambient background. **Back** returns to the top; it
  never closes the launcher.

## Getting the APK

Every push runs the **Build NOVA APK** workflow (`.github/workflows/build-apk.yml`), which:

1. builds a signed, minified release APK,
2. attaches it to the workflow run as the artifact `NOVA-launcher-apk` (a zip), and
3. publishes it as **`NOVA-launcher.apk`** on the rolling release **"NOVA latest build"** (tag `nova-latest`).

Every build is signed with the same prototype key (`app/nova-prototype.keystore`), so a new build
installs over the old one without uninstalling.

## Installing on the TCL TV without a computer

See the install section in the session notes, or briefly:

1. **On the TV**: Settings → System → About → click **Android TV OS build** 7 times to enable Developer options.
2. Install **Downloader** (by AFTVnews) from the Google Play Store on the TV.
3. Get the APK onto the TV:
   - **If the repo is public**: in Downloader, enter
     `https://github.com/polishduk-ctrl/nova-tv-launcher/releases/download/nova-latest/NOVA-launcher.apk`
   - **If the repo stays private**: the TV can't download it directly (GitHub requires a login). On
     your iPhone, open the release in Safari and download `NOVA-launcher.apk` to Files, then send it to
     the TV with a Wi-Fi transfer app such as **Send Files to TV** or **LocalSend** (on both iPhone and
     TV), or upload it to a cloud drive and open the share link in Downloader.
4. When prompted, allow **Downloader** (or your file app) to *install unknown apps*, then install.
5. NOVA appears in the TV's app row.

### Making NOVA your Home screen (no computer needed)

Google TV ignores the normal "default Home app" setting, so NOVA does what you'd otherwise do with
a computer and `adb`. It does this from the TV itself, over the TV's own debugging service:

1. Settings → System → About → select **Android TV OS build** 7 times (enables Developer options).
2. Settings → System → Developer options → turn on **USB debugging**.
3. Open NOVA, select the ✦ button (top right) → **Make NOVA Home**. When the TV asks to allow
   debugging, tick **Always allow** and choose **Allow**.

NOVA then sets itself as the preferred Home activity and switches off the stock launcher package(s)
(it lists them before you confirm). It never touches Settings. To undo, open ✦ → **Restore original launcher**.

> ⚠️ Always use **Restore** before uninstalling NOVA, or the TV will have no Home screen. If that
> ever happens, run `adb shell pm enable com.google.android.apps.tv.launcherx` from a computer
> (or reinstall NOVA and press Restore).

## Building locally

Requires JDK 17 and the Android SDK (API 35): `./gradlew assembleRelease`.
