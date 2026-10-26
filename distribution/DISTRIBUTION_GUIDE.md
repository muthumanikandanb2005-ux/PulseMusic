# Pulse Music Multi-Platform Distribution & Store Submission Guide

This document lists the status, repository URLs, direct links, and submission templates for all open-source repositories and Android app stores.

---

## 1. GitHub Releases (Primary Official Release Channel)
- **Status**: **LIVE & ACTIVE**
- **Repository**: [muthumanikandanb2005-ux/PulseMusic](https://github.com/muthumanikandanb2005-ux/PulseMusic)
- **Latest Release**: [v3.2.8 Release](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v3.2.8)
- **Direct Universal APK Download**: [PulseMusic-Universal.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.2.8/PulseMusic-Universal.apk)
- **Architecture APKs**:
  - [PulseMusic-arm64-v8a.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.2.8/PulseMusic-arm64-v8a.apk)
  - [PulseMusic-armeabi-v7a.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.2.8/PulseMusic-armeabi-v7a.apk)
  - [PulseMusic-x86_64.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.2.8/PulseMusic-x86_64.apk)
  - [PulseMusic-Windows-x64-Setup.exe](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v3.2.8/PulseMusic-Windows-x64-Setup.exe) (Direct Windows 10/11 Installer)

---

## 2. Orion Store (Official Submission)
- **Status**: **LIVE & APPROVED**
- **Warehouse Repository**: [RookieEnough/Orion-Data](https://github.com/RookieEnough/Orion-Data)
- **Submission Issue**: [#1819 - App Submission [1 App] - Pulse Music](https://github.com/RookieEnough/Orion-Data/issues/1819)
- **Package Name**: `com.pulse.music`
- **Integration**: Auto-updates via GitHub Release asset keyword `apk` / `Universal`.

---

## 3. Obtainium (One-Click Direct GitHub App Manager)
Obtainium allows users to get direct background updates directly from GitHub Releases without third-party app stores.
- **Deep Link**: `obtainium://add/https://github.com/muthumanikandanb2005-ux/PulseMusic`
- **Configuration**:
  - Source: GitHub
  - Package Name: `com.pulse.music`
  - APK Filter: `PulseMusic-Universal.apk` (or `PulseMusic-arm64-v8a.apk`)

---

## 4. IzzyOnDroid (F-Droid Community Repository)
IzzyOnDroid is the most popular, fast-inclusion binary F-Droid repository that monitors GitHub releases.
- **Submission Tracker**: [IzzyOnDroid Codeberg Issues](https://codeberg.org/IzzyOnDroid/repo/issues/new)
- **Title**: `[App]: Pulse Music (com.pulse.music)`
- **Body Template**: See `distribution/IZZYONDROID_SUBMISSION.md`

---

## 5. Official F-Droid (Source Build)
Official F-Droid builds apps directly from source.
- **Metadata File**: `distribution/metadata/com.pulse.music.yml`
- **Submission Method**: Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), add `metadata/com.pulse.music.yml`, and open a Merge Request.
