# Pulse Music Multi-Platform Distribution & Store Submission Guide

This document lists the status, repository URLs, direct links, and submission templates for all open-source repositories and Android app stores.

---

## 1. GitHub Releases (Primary Official Release Channel)
- **Status**: **LIVE & ACTIVE**
- **Repository**: [muthumanikandanb2005-ux/PulseMusic](https://github.com/muthumanikandanb2005-ux/PulseMusic)
- **Latest Release**: [v2.2.4 Release](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v2.2.4)
- **Direct Universal APK Download**: [PulseMusic-Universal.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v2.2.4/PulseMusic-Universal.apk)
- **Architecture APKs**:
  - [PulseMusic-arm64-v8a.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v2.2.4/PulseMusic-arm64-v8a.apk)
  - [PulseMusic-armeabi-v7a.apk](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v2.2.4/PulseMusic-armeabi-v7a.apk)
  - [PulseMusic-Windows-x64.zip](https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v2.2.4/PulseMusic-Windows-x64.zip)

---

## 2. Orion Store (Official Submission)
- **Status**: **SUBMITTED**
- **Warehouse Repository**: [RookieEnough/Orion-Data](https://github.com/RookieEnough/Orion-Data)
- **Submission Issue**: [#1819 - App Submission [1 App] - Pulse Music](https://github.com/RookieEnough/Orion-Data/issues/1819)
- **Integration**: Auto-updates via GitHub Release asset keyword `Universal`.

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
IzzyOnDroid is the most popular, fast-inclusion binary F-Droid repository that monitors your GitHub releases.
- **Submission Tracker**: [IzzyOnDroid GitLab Issues](https://gitlab.com/IzzyOnDroid/repo/-/issues/new)
- **Title**: `[App]: Pulse Music (com.pulse.music)`
- **Body Template**:
  ```markdown
  ### Application Details
  - **Application Name**: Pulse Music
  - **Package Name**: com.pulse.music
  - **License**: GPL-3.0-or-later
  - **Repository URL**: https://github.com/muthumanikandanb2005-ux/PulseMusic
  - **Issue Tracker**: https://github.com/muthumanikandanb2005-ux/PulseMusic/issues
  - **Releases**: https://github.com/muthumanikandanb2005-ux/PulseMusic/releases
  - **APK Name pattern**: PulseMusic-Universal.apk
  - **Fastlane structure**: Present in `fastlane/metadata/android/`
  ```

---

## 5. Official F-Droid (Source Build)
Official F-Droid builds apps directly from source.
- **Metadata File**: `distribution/metadata/com.pulse.music.yml`
- **Submission Method**: Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), add `metadata/com.pulse.music.yml`, and open a Merge Request.

---

## 6. APKPure
APKPure requires manual developer account login and review:
- **Developer Console**: [APKPure Developer Portal](https://developer.apkpure.com/)
- **Upload File**: `dist/PulseMusic-Universal.apk`
- **App Name**: `Pulse Music`
- **Package Name**: `com.pulse.music`

---

## 7. APKMirror
APKMirror hosts vetted APKs from developers:
- **Submission Form**: [APKMirror Upload](https://www.apkmirror.com/apk-submission/)
- **APK URL**: `https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v2.2.4/PulseMusic-Universal.apk`
- **Release Page**: `https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v2.2.4`
