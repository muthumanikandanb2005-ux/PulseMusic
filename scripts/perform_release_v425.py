import os
import sys
import shutil
import hashlib
import json
import zipfile
import subprocess
from datetime import datetime, timezone

REPO_ROOT = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic"
RELEASE_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.2.5"
FDROID_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\fdroid-repo"
FDROID_REPO = os.path.join(FDROID_DIR, "repo")
DIST_DIR = os.path.join(REPO_ROOT, "dist")
APK_OUT_DIR = os.path.join(REPO_ROOT, "androidApp", "build", "outputs", "apk", "release")

VERSION = "4.2.5"
VERSION_CODE = 125
TAG = f"v{VERSION}"


def sha256_file(filepath: str) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def step(msg: str) -> None:
    print(f"\n========================================\n[STEP] {msg}\n========================================")


def main() -> None:
    os.makedirs(DIST_DIR, exist_ok=True)
    os.makedirs(RELEASE_DIR, exist_ok=True)
    os.makedirs(FDROID_REPO, exist_ok=True)

    step("1. Verifying Android Release APKs & Windows Installer")
    apk_sources = {
        "universal": os.path.join(APK_OUT_DIR, "androidApp-universal-release.apk"),
        "arm64": os.path.join(APK_OUT_DIR, "androidApp-arm64-v8a-release.apk"),
        "armeabi": os.path.join(APK_OUT_DIR, "androidApp-armeabi-v7a-release.apk"),
        "x86_64": os.path.join(APK_OUT_DIR, "androidApp-x86_64-release.apk"),
    }

    for arch, path in apk_sources.items():
        if not os.path.isfile(path):
            print(f"Error: APK for {arch} not found at {path}")
            sys.exit(1)
        print(f"  Found {arch}: {path} ({os.path.getsize(path)} bytes)")

    win_exe = os.path.join(DIST_DIR, "PulseMusic-Windows-x64-Setup.exe")
    if not os.path.isfile(win_exe):
        print(f"Error: Windows Setup installer not found at {win_exe}")
        sys.exit(1)
    print(f"  Found Windows Setup EXE: {win_exe} ({os.path.getsize(win_exe)} bytes)")

    win_zip = os.path.join(DIST_DIR, "PulseMusic-Windows-x64-Setup.zip")
    print(f"  Packaging {win_zip} ...")
    with zipfile.ZipFile(win_zip, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        zf.write(win_exe, arcname="PulseMusic-Windows-x64-Setup.exe")
    print(f"  Created Windows Setup ZIP: {win_zip} ({os.path.getsize(win_zip)} bytes)")

    step("2. Copying APKs and Windows Installers to dist, release, and fdroid directories")
    apk_mappings = [
        (apk_sources["universal"], "PulseMusic-Universal.apk", f"PulseMusic-v{VERSION}-Universal.apk"),
        (apk_sources["arm64"], "PulseMusic-arm64-v8a.apk", f"PulseMusic-v{VERSION}-arm64-v8a.apk"),
        (apk_sources["armeabi"], "PulseMusic-armeabi-v7a.apk", f"PulseMusic-v{VERSION}-armeabi-v7a.apk"),
        (apk_sources["x86_64"], "PulseMusic-x86_64.apk", f"PulseMusic-v{VERSION}-x86_64.apk"),
    ]

    for src, unversioned, versioned in apk_mappings:
        shutil.copy2(src, os.path.join(DIST_DIR, unversioned))
        shutil.copy2(src, os.path.join(DIST_DIR, versioned))
        shutil.copy2(src, os.path.join(RELEASE_DIR, unversioned))
        shutil.copy2(src, os.path.join(FDROID_REPO, versioned))
        print(f"  Copied {unversioned} -> dist, release & fdroid")

    shutil.copy2(win_exe, os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe"))
    shutil.copy2(win_exe, os.path.join(DIST_DIR, f"PulseMusic-v{VERSION}-Windows-x64-Setup.exe"))
    shutil.copy2(win_zip, os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip"))
    shutil.copy2(win_zip, os.path.join(DIST_DIR, f"PulseMusic-v{VERSION}-Windows-x64-Setup.zip"))

    step("3. Generating Checksums (SHA-256)")
    sums = []
    for fname in sorted(os.listdir(RELEASE_DIR)):
        fpath = os.path.join(RELEASE_DIR, fname)
        if os.path.isfile(fpath) and not fname.endswith(".txt") and not fname.endswith(".md"):
            checksum = sha256_file(fpath)
            sums.append(f"{checksum}  {fname}")
            print(f"  {checksum}  {fname}")

    sums_content = "\n".join(sums) + "\n"
    with open(os.path.join(RELEASE_DIR, "SHA256SUMS.txt"), "w", encoding="utf-8") as f:
        f.write(sums_content)
    with open(os.path.join(DIST_DIR, "SHA256SUMS.txt"), "w", encoding="utf-8") as f:
        f.write(sums_content)

    step("4. Updating version.json for Global OTA Broadcast")
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    release_title = (
        f"Pulse Music v{VERSION} - Glitch-Free Audio, Exact Spotify Playback, Ultra-Crisp Covers & Instant Synced Lyrics"
    )
    release_body = f"""# {release_title}

Official multi-platform release and global OTA update for **Android**, **Windows**, **Orion Store**, **F-Droid**, **Obtainium**, and **Web**.

---

### 🔊 Glitch-Free Audio Under Notifications, Multitasking & Background Play
- **Zero Notification / Multitasking Crackling**: Dispatched Android `AudioFocus` callbacks strictly on the Main application looper, replaced hard ducking steps with smooth volume ramps, added a 180ms transient chime guard, and switched to hardware-first MediaCodec decoding with balanced `400ms–1000ms` 16-bit PCM AudioTrack buffers so notifications and app switching never stall or pop audio.
- **No Background Decoder Contention**: Stopped allocating extra background `ExoPlayer` + `AudioTrack` instances during normal non-crossfade playback and guarded USB DAC routing so notification route checks never reset audio output mid-song.

### 🎯 Exact Spotify Song Playback & Zero Home Refresh
- **Accurate Per-Track Stream Matching**: Validated JioSaavn and YouTube candidates using strict normalized title, artist token, variant (remix/lofi), and duration scoring across all results, plus auto-invalidated stale pre-v4.2.5 Spotify cache entries so every Spotify song plays its exact audio.
- **Instant Playback Without Home Reload**: Tapping any Spotify song or cover on Home plays immediately without triggering a Home shimmer refresh.

### 🖼️ Full-Resolution Unblurred Album Artwork
- **Razor-Sharp 640px / 1080px Covers**: Eliminated low-res `64x64` Spotify thumbnails (`ab67616d00004851`), upgraded Spotify/Google/Saavn CDN URLs to full resolution, and enabled `Size.ORIGINAL` + `FilterQuality.High` rendering across Home, Playlists, MiniPlayer, and Now Playing.

### ⚡ Instant Startup, 120Hz Motion & Zero-Delay Synced Lyrics
- **Single-Pass Instant Home Startup**: Consolidated Home startup collectors into a unified trigger without artificial delays.
- **Snappy Navigation, Text & MiniPlayer**: Refined shared-axis transitions, reduced fade delays, and sped up MiniPlayer track/color transitions.
- **Lightning-Fast Synced Lyrics**: Queried LRCLIB, BiniLyrics, and Lyrica concurrently to render the first synced lyrics immediately, upgraded playhead & canvas subtitle sync to sub-frame precision, and tightened scroll & focus transitions.
"""

    version_data = {
        "version": VERSION,
        "versionCode": VERSION_CODE,
        "downloadUrl": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Universal.apk",
        "download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Universal.apk",
        "mirrorDownloadUrl": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/download/{TAG}/PulseMusic-Universal.apk",
        "mirror_download_url": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/download/{TAG}/PulseMusic-Universal.apk",
        "url": f"https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/tags/{TAG}",
        "mirror_url": f"https://api.github.com/repos/muthumanikandanb2005-ux/Pulse-Music/releases/tags/{TAG}",
        "html_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/{TAG}",
        "mirror_html_url": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/tag/{TAG}",
        "tag_name": TAG,
        "name": release_title,
        "draft": False,
        "prerelease": False,
        "published_at": now_iso,
        "body": release_body,
        "changelog": [
            "Audio Stability: Fixed notification/multitasking/background audio crackling with main-looper AudioFocus handling, smooth volume ducking ramps, hardware-first decoding, and stable PCM16 AudioTrack buffers",
            "Exact Spotify Playback: Added strict title/artist/duration matching across JioSaavn and YouTube so different Spotify songs always play their exact audio",
            "No Home Refresh & Instant Startup: Removed Home shimmer reset on Spotify card taps and consolidated startup fetches",
            "Crisp Full-Res Artwork: Upgraded 64px thumbnails to 640px/1080px HD across Home, Playlists, MiniPlayer, and Now Playing",
            "Instant Synced Lyrics & Smooth Transitions: Parallelized lyrics fetching, eliminated scroll/blur delay with smooth frame playhead tracking, and refined screen & MiniPlayer motion"
        ],
        "assets": [
            {
                "name": "PulseMusic-Universal.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Universal.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["universal"]),
            },
            {
                "name": "PulseMusic-arm64-v8a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-arm64-v8a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["arm64"]),
            },
            {
                "name": "PulseMusic-armeabi-v7a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-armeabi-v7a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["armeabi"]),
            },
            {
                "name": "PulseMusic-x86_64.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-x86_64.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["x86_64"]),
            },
            {
                "name": "PulseMusic-Windows-x64-Setup.exe",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe",
                "content_type": "application/vnd.microsoft.portable-executable",
                "size": os.path.getsize(win_exe),
            },
            {
                "name": "PulseMusic-Windows-x64-Setup.zip",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.zip",
                "content_type": "application/zip",
                "size": os.path.getsize(win_zip),
            },
        ],
        "windowsDownloadUrl": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe",
        "windows_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe",
    }

    with open(os.path.join(REPO_ROOT, "version.json"), "w", encoding="utf-8") as f:
        json.dump(version_data, f, indent=2)
    print("  PulseMusic/version.json updated successfully")

    with open(os.path.join(FDROID_DIR, "version.json"), "w", encoding="utf-8") as f:
        json.dump(version_data, f, indent=2)
    print("  fdroid-repo/version.json updated successfully")

    step("5. Updating F-Droid & Orion Store Repository")
    index_path = os.path.join(FDROID_REPO, "index-v1.json")
    if os.path.isfile(index_path):
        with open(index_path, "r", encoding="utf-8") as f:
            idx = json.load(f)

        idx.setdefault("repo", {})
        idx["repo"]["timestamp"] = int(datetime.now(timezone.utc).timestamp() * 1000)

        app_entry = idx.get("apps", [{}])[0] if idx.get("apps") else {}
        app_entry["suggestedVersionCode"] = VERSION_CODE
        app_entry["suggestedVersionName"] = VERSION
        app_entry["lastUpdated"] = int(datetime.now(timezone.utc).timestamp() * 1000)

        packages = idx.setdefault("packages", {})
        pkg_list = packages.setdefault("com.pulse.music", [])

        for _, apk_file in [
            ("Universal", f"PulseMusic-v{VERSION}-Universal.apk"),
            ("arm64-v8a", f"PulseMusic-v{VERSION}-arm64-v8a.apk"),
            ("armeabi-v7a", f"PulseMusic-v{VERSION}-armeabi-v7a.apk"),
            ("x86_64", f"PulseMusic-v{VERSION}-x86_64.apk"),
        ]:
            apk_path = os.path.join(FDROID_REPO, apk_file)
            if os.path.isfile(apk_path):
                pkg_list.insert(
                    0,
                    {
                        "added": int(datetime.now(timezone.utc).timestamp() * 1000),
                        "apkName": apk_file,
                        "hash": sha256_file(apk_path),
                        "hashType": "sha256",
                        "minSdkVersion": 26,
                        "packageName": "com.pulse.music",
                        "size": os.path.getsize(apk_path),
                        "targetSdkVersion": 34,
                        "versionCode": VERSION_CODE,
                        "versionName": VERSION,
                    },
                )

        with open(index_path, "w", encoding="utf-8") as f:
            json.dump(idx, f, indent=2)
        print("  F-Droid / Orion Store index-v1.json updated")

    step("6. Committing & Pushing Code + OTA Manifest to Git Repos")
    subprocess.run(["git", "add", "-A"], cwd=REPO_ROOT, check=True)
    subprocess.run(
        ["git", "commit", "-m", f"release: Pulse Music v{VERSION} (Build {VERSION_CODE}) OTA and Store release"],
        cwd=REPO_ROOT,
        check=True,
    )
    subprocess.run(["git", "push", "origin", "main"], cwd=REPO_ROOT, check=True)
    subprocess.run(["git", "push", "mirror", "main"], cwd=REPO_ROOT, check=True)
    print("  PulseMusic and Pulse-Music code and OTA manifests pushed to main")

    assets = [
        os.path.join(RELEASE_DIR, "PulseMusic-Universal.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-arm64-v8a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-armeabi-v7a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-x86_64.apk"),
        os.path.join(RELEASE_DIR, "SHA256SUMS.txt"),
        os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe"),
        os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip"),
    ]

    for repo_slug in ("muthumanikandanb2005-ux/PulseMusic", "muthumanikandanb2005-ux/Pulse-Music"):
        step(f"7. Publishing GitHub Release {TAG} on {repo_slug}")
        rel_cmd = [
            "gh",
            "release",
            "create",
            TAG,
            *assets,
            "--target",
            "main",
            "--title",
            release_title,
            "--notes",
            release_body,
            "--repo",
            repo_slug,
        ]
        p = subprocess.run(rel_cmd, capture_output=True, text=True)
        if p.returncode != 0:
            print(f"Error creating release on {repo_slug}: {p.stderr}")
            sys.exit(p.returncode)
        print(f"  GitHub Release {TAG} published on {repo_slug}: {p.stdout.strip()}")

    step("8. Pushing F-Droid / Orion Store Updates")
    if os.path.isdir(os.path.join(FDROID_DIR, ".git")):
        subprocess.run(["git", "add", "-A"], cwd=FDROID_DIR, check=True)
        subprocess.run(
            ["git", "commit", "-m", f"Update repository for Pulse Music v{VERSION} (Build {VERSION_CODE})"],
            cwd=FDROID_DIR,
            check=True,
        )
        subprocess.run(["git", "push", "origin", "main"], cwd=FDROID_DIR, check=True)
        print("  F-Droid repository committed and pushed to GitHub")

    step(f"ALL STEPS COMPLETED! {TAG} RELEASE IS LIVE!")


if __name__ == "__main__":
    main()
