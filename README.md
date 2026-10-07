<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Pulse Music Logo" />
  <h1>Pulse Music</h1>
  <p><strong>A high-performance, open-source streaming music client for Android & Desktop featuring real-time synchronized Apple Music and Spotify styled lyrics, continuous seamless playback, and zero freezing.</strong></p>

  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://img.shields.io/github/v/release/muthumanikandanb2005-ux/PulseMusic?style=flat-square&color=blue" alt="Latest Release" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases"><img src="https://img.shields.io/github/downloads/muthumanikandanb2005-ux/PulseMusic/total?style=flat-square&color=green" alt="Total Downloads" /></a>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-orange.svg?style=flat-square" alt="License: GPL-3.0" /></a>
    <a href="https://github.com/RookieEnough/Orion-Data/issues/1819"><img src="https://img.shields.io/badge/Orion%20Store-Approved-purple?style=flat-square" alt="Orion Store" /></a>
  </p>

  <h4>📥 Download Latest Build</h4>
  <p>
    <a href="https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/latest"><img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" width="190" alt="GitHub Releases" /></a>
  </p>
</div>

---

## ✨ Features

- 🎵 **Ad-Free Music Streaming**: Seamless playback powered by YouTube Music and local audio sources.
- ⚡ **Continuous Zero-Freeze Engine**: Decoupled asynchronous audio pipeline prevents player stuttering, buffer starvation, and skip lag.
- ✨ **Pulse Moving Light & Dynamic Glow Lyrics**:
  - Word-by-word synchronized karaoke glow and dynamic spotlight bloom.
  - Progressive depth-of-field Gaussian blur on non-active lines.
  - Integration with LRCLIB (`/api/get` exact matching), Spotify lyrics API, and Romanization for 12+ languages.
- 🎨 **Modern Expressive UI**:
  - Full Compose Multiplatform UI across Android and Desktop.
  - Apple Music-style dynamic animated gradient backgrounds.
  - Material 3 Expressive and Classic playback themes.
- 🎚️ **Pro Audio & Equalizer**: 10-band equalizer, AutoEq headphone profiles, Reverb, Delay, and Crossfade.
- 📊 **Listening Analytics**: On-device listening habits, monthly recap playlists, and Pulse Wrapped.

---

## 📸 Screenshots

<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="30%" alt="Screenshot 1" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="30%" alt="Screenshot 2" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="30%" alt="Screenshot 3" />
</div>

<br/>

<div align="center">
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="30%" alt="Screenshot 4" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" width="30%" alt="Screenshot 5" />
  <img src="https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" width="30%" alt="Screenshot 6" />
</div>

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17 or higher
- Android SDK (API 35+)

### Clone & Build
```bash
# Clone the repository
git clone --recurse-submodules https://github.com/muthumanikandanb2005-ux/PulseMusic.git
cd PulseMusic

# Assemble Android Release APK
./gradlew :androidApp:assembleRelease

# Run Desktop Application
./gradlew :desktopApp:run
```

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
