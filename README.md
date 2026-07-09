# World Cup IPTV Player

**Ad-free Android IPTV player with Media3/ExoPlayer, HLS/M3U8 support, native playlist parsing, and dead-channel auto-skip.**

---

## 📱 Screenshots / Features
- Auto-skips unavailable streams after playback failure
- Manual M3U/M3U8 URL entry + primary/secondary sources
- Picture-in-picture mode
- OLED black UI
- Auto-rotation handling via `OrientationEventListener`
- Browser user-agent spoofing for blocked IPTV sources
- Cleartext and HTTPS stream support
- No Google Mobile Ads SDK

## 🔧 Tech Stack
- **Language:** Kotlin
- **Player:** Media3 / ExoPlayer `1.3.1`
- **Datasource:** `media3-datasource:1.3.1`, `media3-exoplayer-hls:1.3.1`
- **Parsing:** Native C++ M3U/M3U8 parser via JNI + Kotlin fallback parser
- **Networking:** OkHttp + Cloudflare DoH (`1.1.1.1` / `1.0.0.1`) fallback to `DefaultHttpDataSource`
- **Storage:** DataStore for playlist URL caching
- **Target:** Android 16 (SDK 35)
- **Min SDK:** 26

## 📦 Installation
Prebuilt APKs are available under **GitHub Releases**:
1. Go to the [Releases](../../releases) page
2. Download `worldcup-iptv-player.apk` from the latest release
3. Install:
   ```bash
   adb install -r worldcup-iptv-player.apk
   ```
4. Launch:
   ```bash
   adb shell monkey -p com.example.iptvplayer -c android.intent.category.LAUNCHER 1
   ```

## 🛠️ Build from Source
```bash
git clone https://github.com/MDMisba97khan/worldcup-iptv-player.git
cd worldcup-iptv-player
./gradlew assembleRelease
```

## ✅ Current Stable Build
- **Tag:** `v1.0.0-build-112`
- **Commit:** `54f1375d793b211aa06ef28c987450150fd4ca26`
- **Status:** Stable, in active test use
- **Last verified device:** `0N14520V24104E02` (Android 16)

## 🧪 Recent Fixes & Validation
| Issue | Fix | Status |
|---|---|---|
| `WRITE_SETTINGS` `SecurityException` on startup | Removed system-settings write; keep `OrientationEventListener` only | ✅ Fixed |
| Auto-rotation crash | CI `29052939035` success, on-device verified | ✅ Fixed |
| Manual M3U URL input non-functional | URL field loads and advances playback | ✅ Fixed |
| Dead streams causing crashes | Caught as `Source error`, auto-advances to next channel | ✅ Fixed |
| HLS `No suitable media source factory found` | Added `media3-exoplayer-hls:1.3.1` | ✅ Fixed |
| 404 / dead channel blocking | Auto-skip + player reinit on playback error | ✅ Fixed |

## ⚠️ Known Pending Items
- Fullscreen toggle button behavior refinement
- Multi-source M3U failover UX (primary/secondary ordering present)

## 🔒 Signing
APK is signed with `release.keystore`, alias `release`. Do not redistribute unsigned debug builds.

## 📡 Networking Notes
- Browser user-agent spoofing active to bypass IPTV blocks
- `android:usesCleartextTraffic="true"` enabled for HTTP/HLS streams
- Cloudflare DoH configured via OkHttp with fallback to `DefaultHttpDataSource`

## 🧭 CI
GitHub Actions builds are triggered from `main`. Download artifacts from:
```bash
gh run download <run_id> --repo MDMisba97khan/worldcup-iptv-player --dir /tmp
```

## 📄 License
Add your license choice here.
