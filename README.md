<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Pulse Music Player app logo" />
  <h1>Pulse Music Player</h1>
  <p><strong>An open-source Android music player with synchronized lyrics, local audio playback, music streaming integrations, and a modern Compose Multiplatform interface for Android and Desktop.</strong></p>

  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://img.shields.io/github/v/release/muthumanikandanb2005-ux/PulseMusic?style=flat-square&color=blue" alt="Latest Release" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases"><img src="https://img.shields.io/github/downloads/muthumanikandanb2005-ux/PulseMusic/total?style=flat-square&color=green" alt="Total Downloads" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square" alt="License: GPL-3.0" /></a>
    <a href="https://muthumanikandanb2005-ux.github.io/fdroid-repo/"><img src="https://img.shields.io/badge/F--Droid-PulseMusic%20Repo-3DDC84?logo=f-droid&logoColor=white&style=flat-square" alt="F-Droid Repository" /></a>
    <a href="https://apt.izzysoft.de/fdroid/"><img src="https://img.shields.io/badge/IzzyOnDroid-F--Droid%20Repo-blue?logo=f-droid&logoColor=white&style=flat-square" alt="IzzyOnDroid" /></a>
    <a href="https://accrescent.app"><img src="https://img.shields.io/badge/Accrescent-Store-4CAF50?logo=android&logoColor=white&style=flat-square" alt="Accrescent" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic"><img src="https://img.shields.io/badge/Obtainium-Supported-2196F3?logo=android&logoColor=white&style=flat-square" alt="Obtainium" /></a>
    <a href="https://openapk.net"><img src="https://img.shields.io/badge/OpenAPK-FOSS-orange?style=flat-square" alt="OpenAPK" /></a>
    <a href="https://github.com/RookieEnough/Orion-Data/issues/1819"><img src="https://img.shields.io/badge/Orion%20Store-Approved-purple?style=flat-square" alt="Orion Store" /></a>
  </p>

  <h4>Download & Install Pulse Music Player</h4>
  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" width="170" alt="Download Pulse Music Player from GitHub Releases" /></a>
    <a href="https://muthumanikandanb2005-ux.github.io/fdroid-repo/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="170" alt="Get Pulse Music Player on F-Droid" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic#install-via-obtainium"><img src="https://img.shields.io/badge/Obtainium-Add_App-2196F3?style=for-the-badge&logo=android" height="42" alt="Install via Obtainium" /></a>
  </p>
</div>

---

## About Pulse Music Player

Pulse Music Player is an open-source music player project for Android, with a desktop version. It focuses on a modern listening experience with synchronized song lyrics, music library playback, audio effects, and listening statistics. Explore the source code, review the features, or build the Android app from source using Android Studio.

## Features

- 🎵 **Music Playback**: Play music from supported streaming integrations and local audio sources.
- ⚡ **Playback Pipeline**: Asynchronous audio processing designed to reduce stuttering, buffer starvation, and skip lag.
- ✨ **Synchronized Lyrics**:
  - Word-by-word karaoke highlighting and dynamic lyric effects.
  - LRCLIB integration, Spotify lyrics API integration, and romanization support for 12+ languages.
- 🎨 **Modern Interface**:
  - Compose Multiplatform UI for Android and Desktop.
  - Animated gradient backgrounds.
  - Material 3 Expressive and Classic playback themes.
- 🎚️ **Audio Effects and Equalizer**: 10-band equalizer, AutoEq headphone profiles, reverb, delay, and crossfade.
- 📊 **Listening Analytics**: On-device listening insights, monthly recap playlists, and Pulse Wrapped.

## Screenshots

<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="19%" alt="Pulse Music Player Android home screen" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="19%" alt="Pulse Music Player now playing screen" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="19%" alt="Synchronized lyrics in Pulse Music Player" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="19%" alt="Listen Together screen in Pulse Music Player" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="19%" alt="Pulse Music Player settings screen" />
</div>

---

## Build Pulse Music Player from Source

### Requirements

- JDK 17 or higher
- Android SDK (API 35+)

### Clone and Build

```bash
# Clone the repository, including submodules
git clone --recurse-submodules https://github.com/muthumanikandanb2005-ux/PulseMusic.git
cd PulseMusic

# Assemble the Android release APK
./gradlew :androidApp:assembleRelease

# Run the desktop application
./gradlew :desktopApp:run
```

## Author & Developer

<div align="center">
  <a href="https://github.com/muthumanikandanb2005-ux">
    <img src="https://avatars.githubusercontent.com/u/192257257?v=4" width="120" height="120" style="border-radius: 50%;" alt="Muthumanikandan B" />
  </a>
  <h3><a href="https://github.com/muthumanikandanb2005-ux">Muthumanikandan B</a></h3>
  <p><strong>Owner, Creator & Sole Lead Developer</strong></p>
  <p><em>Pulse Music Studio</em></p>
</div>

## Distribution & App Stores

### 📱 Install via Obtainium
1. Install [Obtainium](https://github.com/ImranR98/Obtainium) on your Android device.
2. Tap **Add App** and paste: `https://github.com/muthumanikandanb2005-ux/PulseMusic`
3. Filter APK: `PulseMusic-v.*-Universal.apk` (or import [obtainium.json](obtainium.json)).
4. Tap **Save** & **Install** — Obtainium will notify and update Pulse Music automatically on new releases.

### 📦 IzzyOnDroid (F-Droid Compatible)
Pulse Music conforms to Fastlane metadata standards (`fastlane/metadata/android/` and `metadata/`). Releases are available via F-Droid client when IzzyOnDroid repository is enabled.

### 🛡️ Accrescent
Pulse Music is built for modern Android architectures (API 24+) with verifiable APK signing:
- **Package Name:** `com.pulse.music`
- **Signing SHA-256:** `18:A3:E0:1D:32:EF:F9:8D:32:22:CB:50:F5:20:0A:E0:F7:ED:D1:8F:C4:77:F5:3D:3F:F9:2F:55:26:B6:9D:88`

### 🌐 OpenAPK
Pulse Music is indexed in OpenAPK with direct open-source package downloads matching official GitHub releases.

---

## Contributing

Contributions, bug reports, and suggestions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, development workflows, and how to submit pull requests.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](https://github.com/muthumanikandanb2005-ux/PulseMusic/blob/main/LICENSE) for details.

## Links

- **Source code:** https://github.com/muthumanikandanb2005-ux/PulseMusic
- **Latest release:** https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest
- **F-Droid repository:** https://muthumanikandanb2005-ux.github.io/fdroid-repo/
- **Developer Profile:** https://github.com/muthumanikandanb2005-ux
