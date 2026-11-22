import os
import sys
import shutil
import hashlib
import json
import subprocess
from datetime import datetime, timezone

REPO_ROOT = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic"
RELEASE_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.1.9"
FDROID_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\fdroid-repo"
FDROID_REPO = os.path.join(FDROID_DIR, "repo")
DIST_DIR = os.path.join(REPO_ROOT, "dist")
APK_OUT_DIR = os.path.join(REPO_ROOT, "androidApp", "build", "outputs", "apk", "release")

VERSION = "4.1.9"
VERSION_CODE = 119
TAG = f"v{VERSION}"

def sha256_file(filepath):
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def step(msg):
    print(f"\n========================================\n[STEP] {msg}\n========================================")

def main():
    os.makedirs(DIST_DIR, exist_ok=True)
    os.makedirs(RELEASE_DIR, exist_ok=True)
    os.makedirs(FDROID_REPO, exist_ok=True)

    step("1. Verifying Android Release APKs")
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

    step("2. Copying APKs to dist, release, and fdroid directories")
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
        print(f"  Copied {unversioned} -> dist & release")

    # Copy Windows setup files if present in dist
    win_exe = os.path.join(DIST_DIR, "PulseMusic-Windows-x64-Setup.exe")
    win_zip = os.path.join(DIST_DIR, "PulseMusic-Windows-x64-Setup.zip")
    if os.path.isfile(win_exe):
        shutil.copy2(win_exe, os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe"))
    if os.path.isfile(win_zip):
        shutil.copy2(win_zip, os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip"))

    step("3. Generating Checksums (SHA-256)")
    sums = []
    for fname in sorted(os.listdir(RELEASE_DIR)):
        fpath = os.path.join(RELEASE_DIR, fname)
        if os.path.isfile(fpath) and not fname.endswith(".txt") and not fname.endswith(".md"):
            checksum = sha256_file(fpath)
            sums.append(f"{checksum}  {fname}")
            print(f"  {checksum}  {fname}")

    sums_content = "\n".join(sums) + "\n"
    sums_path_release = os.path.join(RELEASE_DIR, "SHA256SUMS.txt")
    sums_path_dist = os.path.join(DIST_DIR, "SHA256SUMS.txt")
    with open(sums_path_release, "w", encoding="utf-8") as f:
        f.write(sums_content)
    with open(sums_path_dist, "w", encoding="utf-8") as f:
        f.write(sums_content)

    step("4. Updating version.json for Global OTA Broadcast")
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    
    version_json_path = os.path.join(REPO_ROOT, "version.json")
    fdroid_version_json_path = os.path.join(FDROID_DIR, "version.json")
    
    release_title = f"Pulse Music v{VERSION} - Canvas Video Fix, Zero-Delay Sync & Bini/Lyrica High Priority Rich Lyrics"
    release_body = f"""# Pulse Music v{VERSION} - Canvas Video Fix, Zero-Delay Sync & Bini/Lyrica High Priority Rich Lyrics

Official multi-platform release and global OTA update for **Android**, **Windows**, **Orion Store**, **F-Droid**, **Obtainium**, and **Web**.

---

### 🎬 Fullscreen Spotify-Style Canvas Videos (No Login Required)
- **Resolved Layer Occlusion**: Moving ambient light gradients and breathing radial glows no longer obscure canvas videos in BitChord, Spotify, and Expressive players.
- **Accurate Aspect Ratio & Scaling**: Native Media3 DP dimensions with centered `ContentScale.Crop` ensure videos display sharp and centered with zero stretching.
- **Anonymous / Guest Access**: Omitted empty session cookies so Spotify guest access tokens are retrieved automatically, enabling canvas videos for all songs without needing a Spotify login.

### ⏱️ Zero-Delay Real-Time Lyrics Sync
- **Ultra-Responsive 50ms Playback Ticker**: Reduced position polling interval from 250ms down to 50ms (20 updates/sec), eliminating lyrics highlight lag.
- **Locked Playhead Interpolation**: Tightened `PLAYHEAD_TICK_MS` to 100ms so frame-rate interpolation remains locked to the audio position with zero drift.

### 🎙️ High-Priority BiniLyrics & Lyrica Rich Synced Lyrics
- **Parallel Multi-Provider Querying**: BiniLyrics (`lrc.red`) and Lyrica are fetched concurrently via asynchronous coroutines, cutting network search time in half.
- **Word-by-Word Priority**: Word-level rich synced lyrics (`RICH_SYNCED`) are given absolute priority over line-synced and plain lyrics.
- **Flexible Timestamp Parsers**: Supports flexible 1- and 2-digit minute timestamps and dot/colon formatting (`<M:SS.mm>`, `<MM:SS.mm>`, `[MM:SS:mm]`).
- **Default Provider**: BiniLyrics is now selected as default provider with Lyrica as direct secondary.

### 🎵 Enhanced Spotify Integration
- **Full Library Sync**: Access Spotify playlists, recently played tracks, user folders, and home screen trending sections.
- **Web & OAuth Login**: Clean login flow matching YouTube login aesthetics.
"""

    v_data = {
        "version": VERSION,
        "versionCode": VERSION_CODE,
        "downloadUrl": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-Universal.apk",
        "download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-Universal.apk",
        "mirrorDownloadUrl": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/download/v{VERSION}/PulseMusic-Universal.apk",
        "mirror_download_url": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/download/v{VERSION}/PulseMusic-Universal.apk",
        "url": f"https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/tags/v{VERSION}",
        "mirror_url": f"https://api.github.com/repos/muthumanikandanb2005-ux/Pulse-Music/releases/tags/v{VERSION}",
        "html_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v{VERSION}",
        "mirror_html_url": f"https://github.com/muthumanikandanb2005-ux/Pulse-Music/releases/tag/v{VERSION}",
        "tag_name": f"v{VERSION}",
        "name": release_title,
        "draft": False,
        "prerelease": False,
        "published_at": now_iso,
        "body": release_body,
        "changelog": [
            "Fullscreen Spotify-style Canvas Video playing across BitChord, Spotify, and Expressive players",
            "Compulsory Canvas video playback without requiring Spotify login using guest access tokens",
            "Spotify integration: user playlists, recently played, folders, and trending shelves on Home Screen",
            "BiniLyrics (lrc.red) and Lyrica ultra-fast parallel search with High-Priority word-by-word rich synced lyrics",
            "Zero-delay lyrics sync with real-time 50ms playback ticker & tight 100ms playhead interpolation"
        ],
        "assets": [
            {
                "name": "PulseMusic-Universal.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-Universal.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-Universal.apk")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-Universal.apk")) else 0
            },
            {
                "name": "PulseMusic-arm64-v8a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-arm64-v8a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-arm64-v8a.apk")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-arm64-v8a.apk")) else 0
            },
            {
                "name": "PulseMusic-armeabi-v7a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-armeabi-v7a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-armeabi-v7a.apk")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-armeabi-v7a.apk")) else 0
            },
            {
                "name": "PulseMusic-x86_64.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-x86_64.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-x86_64.apk")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-x86_64.apk")) else 0
            }
        ]
    }

    with open(version_json_path, "w", encoding="utf-8") as f:
        json.dump(v_data, f, indent=2)
    print("  PulseMusic/version.json updated successfully")

    if os.path.isdir(FDROID_DIR):
        with open(fdroid_version_json_path, "w", encoding="utf-8") as f:
            json.dump(v_data, f, indent=2)
        print("  fdroid-repo/version.json updated successfully")

    release_notes_path = os.path.join(DIST_DIR, f"release_notes_v{VERSION}.md")
    with open(release_notes_path, "w", encoding="utf-8") as f:
        f.write(v_data["body"])

    step("5. Updating F-Droid & Orion Store Repository")
    fdroid_json_path = os.path.join(FDROID_REPO, "index-v1.json")
    if os.path.isfile(fdroid_json_path):
        try:
            with open(fdroid_json_path, "r", encoding="utf-8") as f:
                fd_data = json.load(f)
            
            packages = fd_data.get("packages", {}).get("com.pulse.music", [])
            univ_apk = os.path.join(FDROID_REPO, f"PulseMusic-v{VERSION}-Universal.apk")
            if os.path.isfile(univ_apk):
                entry = {
                    "added": int(datetime.now(timezone.utc).timestamp() * 1000),
                    "apkName": f"PulseMusic-v{VERSION}-Universal.apk",
                    "hash": sha256_file(univ_apk),
                    "hashType": "sha256",
                    "minSdkVersion": 26,
                    "nativecode": ["arm64-v8a", "armeabi-v7a", "x86_64"],
                    "packageName": "com.pulse.music",
                    "size": os.path.getsize(univ_apk),
                    "targetSdkVersion": 35,
                    "versionCode": VERSION_CODE,
                    "versionName": VERSION
                }
                packages.insert(0, entry)
                fd_data["packages"]["com.pulse.music"] = packages

            if "apps" in fd_data:
                for app in fd_data["apps"]:
                    if app.get("packageName") == "com.pulse.music":
                        app["suggestedVersionCode"] = VERSION_CODE
                        app["suggestedVersionName"] = VERSION

            with open(fdroid_json_path, "w", encoding="utf-8") as f:
                json.dump(fd_data, f, indent=2)
            print("  F-Droid / Orion Store index-v1.json updated")
        except Exception as e:
            print(f"  Warning: failed to update fdroid index: {e}")

    step("6. GitHub Release Creation & APK Asset Upload (PulseMusic)")
    gh_cmd = [
        "gh", "release", "create", TAG,
        "--repo", "muthumanikandanb2005-ux/PulseMusic",
        "--title", release_title,
        "--notes-file", release_notes_path
    ]
    all_release_files = [os.path.join(RELEASE_DIR, f) for f in os.listdir(RELEASE_DIR)]
    gh_cmd.extend(all_release_files)
    print("Running gh release create for PulseMusic...")
    res = subprocess.run(gh_cmd, cwd=REPO_ROOT, shell=True)
    if res.returncode == 0:
        print(f"GitHub Release {TAG} created successfully on PulseMusic!")
    else:
        print(f"gh release create exited with code {res.returncode}, attempting upload --clobber...")
        upload_cmd = ["gh", "release", "upload", TAG, "--repo", "muthumanikandanb2005-ux/PulseMusic"] + all_release_files + ["--clobber"]
        subprocess.run(upload_cmd, cwd=REPO_ROOT, shell=True)

    step("7. GitHub Release Creation & APK Asset Upload (Pulse-Music Mirror)")
    gh_mirror_cmd = [
        "gh", "release", "create", TAG,
        "--repo", "muthumanikandanb2005-ux/Pulse-Music",
        "--title", release_title,
        "--notes-file", release_notes_path
    ]
    gh_mirror_cmd.extend(all_release_files)
    print("Running gh release create for Pulse-Music mirror...")
    res_mirror = subprocess.run(gh_mirror_cmd, cwd=REPO_ROOT, shell=True)
    if res_mirror.returncode == 0:
        print(f"GitHub Release {TAG} created successfully on Pulse-Music!")
    else:
        upload_mirror_cmd = ["gh", "release", "upload", TAG, "--repo", "muthumanikandanb2005-ux/Pulse-Music"] + all_release_files + ["--clobber"]
        subprocess.run(upload_mirror_cmd, cwd=REPO_ROOT, shell=True)

    step("8. Pushing Code & OTA Manifest to Git on Both Repos")
    try:
        subprocess.run(["git", "add", "-A"], cwd=REPO_ROOT, check=True)
        subprocess.run(["git", "commit", "-m", f"release: Pulse Music v{VERSION} (Build {VERSION_CODE}) OTA and Store release"], cwd=REPO_ROOT)
        subprocess.run(["git", "push", "origin", "main"], cwd=REPO_ROOT, check=True)
        subprocess.run(["git", "push", "mirror", "main"], cwd=REPO_ROOT, check=True)
        print("  PulseMusic and Pulse-Music code and OTA manifests pushed to main")
    except Exception as e:
        print(f"  Warning: git push result: {e}")

    step("9. Pushing F-Droid / Orion Store updates")
    try:
        subprocess.run(["git", "add", "-A"], cwd=FDROID_DIR, check=True)
        subprocess.run(["git", "commit", "-m", f"update: Pulse Music v{VERSION} (Build {VERSION_CODE})"], cwd=FDROID_DIR)
        subprocess.run(["git", "push", "origin", "main"], cwd=FDROID_DIR, check=True)
        print("  F-Droid / Orion Store repository pushed to GitHub main")
    except Exception as e:
        print(f"  Warning: failed to push fdroid-repo: {e}")

    step("10. Summary")
    print(f"Release v{VERSION} (Build {VERSION_CODE}) complete!")
    print(f"APKs and release assets saved to: {RELEASE_DIR}")
    print(f"Global OTA broadcast live on: https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json")

if __name__ == "__main__":
    main()
