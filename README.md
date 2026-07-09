# World Cup IPTV Player

**Ad-free Android IPTV player with Media3/ExoPlayer, HLS/M3U8 support, native playlist parsing, and dead-channel auto-skip.**

---

## 📱 Features
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
- **Networking:** OkHttp + Cloudflare DoH (`1.1.1.1` / `1.0.0.1`) with `DefaultHttpDataSource` fallback
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
- **Release:** Latest GitHub Release APK
- **Status:** Stable, in active test use
- **Target device:** Android 16

## 🧪 Recent Fixes & Validation
| Issue | Fix | Status |
|---|---|---|
| `WRITE_SETTINGS` `SecurityException` on startup | Removed system-settings write; keep `OrientationEventListener` only | ✅ Fixed |
| Auto-rotation crash | On-device verified on `OrientationEventListener` | ✅ Fixed |
| Manual M3U URL input non-functional | Verified URL field accepts custom playlist links | ✅ Fixed |
| Dead-stream playback loops | `Source error` handled and auto-skips next channel | ✅ Fixed |
| HLS source factory error | Added `media3-exoplayer-hls:1.3.1` | ✅ Fixed |
| 404 dead channel blocking | Auto-skip + player reinit on playback error | ✅ Fixed |

## ⚠️ Known Pending Items
- Fullscreen toggle button behavior refinement
- Multi-source M3U failover UX (primary/secondary ordering present)

## 🔒 Signing
APK signing is done in CI via GitHub Actions using a base64-encoded `RELEASE_KEYSTORE_BASE64` secret. Passwords are supplied through workflow secrets/env, not committed to the repo.

## 📡 Networking Notes
- Browser user-agent spoofing active to bypass blocked IPTV sources
- `android:usesCleartextTraffic="true"` enabled for HTTP/HLS streams
- Cloudflare DoH configured via OkHttp with `DefaultHttpDataSource` fallback

## 🧭 CI
Builds are published automatically from `main`. Use GitHub Releases for the recommended artifacts.

## 📄 License
Add your license choice here.

## 🛡️ Security & Privacy
- Treat this repo as **containing sensitive supply-chain data**: never open an issue or PR with real tokens, keystore files, passtokens, signing passwords, internal device IDs, CI run IDs, commit hashes related to production chain, or user-specific paths.
- Realm-scoped tokens belong in **GitHub Actions secrets** only; do not commit them.
- APK signing is performed entirely in CI using `RELEASE_KEYSTORE_BASE64`; passwords must be supplied as `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_PASSWORD` secrets, not checked into source control.
- Use **signed GitHub releases** and verify APK signatures before installing on production devices.
- Enable **secret scanning**, **push protection**, and **Dependabot security updates** on the repo.
- Restrict `main` with **required PR review** and **required status checks**.
- Rotate all credentials regularly and use GitHub **fine-grained tokens** with the minimum scopes needed.

## 🔐 Recommended Account Checklist
- Enable **2-factor authentication** on `MDMisba97khan`.
- Use **hardware security key** where possible.
- Do not reuse passwords across GitHub, email, and hosting.
- Review **Authorized OAuth Apps** and **GitHub Apps** monthly.
- Limit token lifetimes; prefer fine-grained PATs with repository-only access.
- Keep contact email private in GitHub public profile.
