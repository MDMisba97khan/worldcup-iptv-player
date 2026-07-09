# World Cup IPTV Player

**Ad-free Android IPTV player with Media3/ExoPlayer, HLS/M3U8 support, dead-channel auto-skip, OLED black UI, and Picture-in-Picture mode.**

---

## 📱 Features
- Auto-skips unavailable streams after playback failure
- Manual M3U/M3U8 URL entry with multi-source support
- Picture-in-picture mode
- OLED black UI
- Auto-rotation handling
- Cleartext and HTTPS stream support
- No Google Mobile Ads SDK

## 🔧 Tech Stack
- **Language:** Kotlin
- **Player:** Media3 / ExoPlayer
- **Networking:** OkHttp with secure HTTP datasource support
- **Storage:** DataStore for playlist URL caching
- **Target:** Modern Android
- **Min SDK:** 26

## 📦 Installation
Prebuilt APKs are available under **GitHub Releases**:
1. Go to the [Releases](../../releases) page
2. Download `worldcup-iptv-player.apk` from the latest release
3. Install with Android package installer
4. Launch the app from your launcher

## 🛠️ Build from Source
```bash
git clone https://github.com/MDMisba97khan/worldcup-iptv-player.git
cd worldcup-iptv-player
./gradlew assembleRelease
```

## 📄 License
Add your license choice here.

## 🛡️ Security
- Do not open issues or PRs with secrets, signing material, tokens, or internal infrastructure details.
- Use signed GitHub releases and verify APK signatures before installing.
- Enable secret scanning, push protection, and Dependabot security updates.
- Restrict `main` with required review and required status checks.
