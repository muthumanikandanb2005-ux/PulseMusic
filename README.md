<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Pulse Music Player app logo" />
  <h1>Pulse Music Player</h1>
  <p><strong>An open-source Android music player with synchronized lyrics, local audio playback, music streaming integrations, and a modern Compose Multiplatform interface for Android and Desktop.</strong></p>

  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://img.shields.io/github/v/release/muthumanikandanb2005-ux/PulseMusic?style=flat-square&color=blue" alt="Latest Release" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases"><img src="https://img.shields.io/github/downloads/muthumanikandanb2005-ux/PulseMusic/total?style=flat-square&color=green" alt="Total Downloads" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square" alt="License: GPL-3.0" /></a>
    <a href="https://muthumanikandanb2005-ux.github.io/fdroid-repo/"><img src="https://img.shields.io/badge/F--Droid-PulseMusic%20Repo-3DDC84?logo=f-droid&logoColor=white&style=flat-square" alt="F-Droid Repository" /></a>
    <a href="https://github.com/RookieEnough/Orion-Data/issues/1819"><img src="https://img.shields.io/badge/Orion%20Store-Approved-purple?style=flat-square" alt="Orion Store" /></a>
  </p>

  <h4>Download Pulse Music Player</h4>
  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" width="190" alt="Download Pulse Music Player from GitHub Releases" /></a>
    <a href="https://muthumanikandanb2005-ux.github.io/fdroid-repo/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="190" alt="Get Pulse Music Player on F-Droid" /></a>
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

## Contributing

Issues, bug reports, and improvements are welcome. Please open an issue in this repository to discuss a bug or proposed change before submitting a pull request.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](https://github.com/muthumanikandanb2005-ux/PulseMusic/blob/main/LICENSE) for details.

## Links

- **Source code:** https://github.com/muthumanikandanb2005-ux/PulseMusic
- **Latest release:** https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest
- **F-Droid repository:** https://muthumanikandanb2005-ux.github.io/fdroid-repo/
