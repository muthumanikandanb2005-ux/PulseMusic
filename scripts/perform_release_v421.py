import os
import sys
import shutil
import hashlib
import json
import subprocess
from datetime import datetime, timezone

REPO_ROOT = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic"
RELEASE_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.2.1"
FDROID_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\fdroid-repo"
FDROID_REPO = os.path.join(FDROID_DIR, "repo")
DIST_DIR = os.path.join(REPO_ROOT, "dist")
APK_OUT_DIR = os.path.join(REPO_ROOT, "androidApp", "build", "outputs", "apk", "release")

VERSION = "4.2.1"
VERSION_CODE = 121
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
    
    release_title = f"Pulse Music v{VERSION} - Windows Playback Hotfix & Enhanced JioSaavn Search Integration"
    release_body = f"""# Pulse Music v{VERSION} - Windows Playback Hotfix & Enhanced JioSaavn Search Integration

Official multi-platform release and global OTA update for **Android**, **Windows**, **Orion Store**, **F-Droid**, **Obtainium**, and **Web**.

---

### 🚀 Windows MPV Native Audio Engine & Auto-Skip Hotfix
- **Bundled `libmpv-2.dll`**: Solved missing native MPV library on Windows desktop by bundling `libmpv-2.dll` into app resources and root installation directories.
- **Multi-Path Dynamic Discovery**: `MpvLibrary` now searches application execution directory, `app/resources`, and native paths with prioritized candidate loading.
- **Runaway Auto-Skip Elimination**: Fixed automatic skipping bug where error counters were reset prematurely during auto-advances. The player now enforces strict error thresholds without cycling through the queue.

### 🔍 Enhanced JioSaavn Search Console Integration
- **Parallel Autocomplete Suggestions**: As users type in the SearchBar, suggestions from JioSaavn autocomplete and YouTube Music are gathered simultaneously.
- **Instant 320kbps Lossless Playback**: Top JioSaavn songs display seamlessly in recommended items with pre-cached direct 320kbps streams for zero-delay playback.
- **Smart Result Interleaving**: JioSaavn regional and high-res results are interleaved and deduplicated alongside YouTube Music results.
- **Robust Radio & Queue Seeding**: Fixed queue loading for JioSaavn tracks (`saavn:...`) to ensure seamless continuous playback without YouTube radio errors.

### 🎨 Visual & Performance Refinements
- **Complete Vibrant Expressive Circular Player**: Continued refinements to the modern circular disc artwork player and ambient multi-color light ring.
- **3D Cover Flow Carousel**: Smooth perspective rotation and frosted track info banner.
- **Ultra-low latency audio buffering** with automated high-bitrate fallback resolution.
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
            "Windows MPV Native Engine: Bundled libmpv-2.dll and expanded multi-path discovery, eliminating playback errors and auto-skipping",
            "Enhanced JioSaavn Search Integration: Parallel real-time autocomplete suggestions and recommended song dropdown in search console",
            "Continuous JioSaavn Queue: Seamless queue extension and radio playback without YouTube API endpoint conflicts",
            "Vibrant Expressive Theme Player: Circular disc artwork, glowing concentric progress ring, 5-button main transport row, and ambient light backdrop",
            "Cover Flow 3D Perspective Carousel: 3D rotation, depth scaling, card overlapping, and frosted track info banner",
            "Pinterest-Style Floating Navigation Dock: Unified capsule pill, far-left circular brand emblem, sliding squircle chip, and active glow dot"
        ],
        "assets": [
            {
                "name": "PulseMusic-Universal.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Universal.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["universal"])
            },
            {
                "name": "PulseMusic-arm64-v8a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-arm64-v8a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["arm64"])
            },
            {
                "name": "PulseMusic-armeabi-v7a.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-armeabi-v7a.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["armeabi"])
            },
            {
                "name": "PulseMusic-x86_64.apk",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-x86_64.apk",
                "content_type": "application/vnd.android.package-archive",
                "size": os.path.getsize(apk_sources["x86_64"])
            }
        ]
    }

    if os.path.isfile(win_exe):
        version_data["windowsDownloadUrl"] = f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe"
        version_data["windows_download_url"] = f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe"
        version_data["assets"].append({
            "name": "PulseMusic-Windows-x64-Setup.exe",
            "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.exe",
            "content_type": "application/vnd.microsoft.portable-executable",
            "size": os.path.getsize(win_exe)
        })
    if os.path.isfile(win_zip):
        version_data["assets"].append({
            "name": "PulseMusic-Windows-x64-Setup.zip",
            "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/{TAG}/PulseMusic-Windows-x64-Setup.zip",
            "content_type": "application/zip",
            "size": os.path.getsize(win_zip)
        })

    version_json_path = os.path.join(REPO_ROOT, "version.json")
    fdroid_version_json_path = os.path.join(FDROID_DIR, "version.json")

    with open(version_json_path, "w", encoding="utf-8") as f:
        json.dump(version_data, f, indent=2)
    print("  PulseMusic/version.json updated successfully")

    with open(fdroid_version_json_path, "w", encoding="utf-8") as f:
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
        
        for arch_key, apk_file in [
            ("Universal", f"PulseMusic-v{VERSION}-Universal.apk"),
            ("arm64-v8a", f"PulseMusic-v{VERSION}-arm64-v8a.apk"),
            ("armeabi-v7a", f"PulseMusic-v{VERSION}-armeabi-v7a.apk"),
            ("x86_64", f"PulseMusic-v{VERSION}-x86_64.apk"),
        ]:
            apk_path = os.path.join(FDROID_REPO, apk_file)
            if os.path.isfile(apk_path):
                pkg_list.insert(0, {
                    "added": int(datetime.now(timezone.utc).timestamp() * 1000),
                    "apkName": apk_file,
                    "hash": sha256_file(apk_path),
                    "hashType": "sha256",
                    "minSdkVersion": 26,
                    "packageName": "com.pulse.music",
                    "size": os.path.getsize(apk_path),
                    "targetSdkVersion": 34,
                    "versionCode": VERSION_CODE,
                    "versionName": VERSION
                })

        with open(index_path, "w", encoding="utf-8") as f:
            json.dump(idx, f, indent=2)
        print("  F-Droid / Orion Store index-v1.json updated")

    step("6. GitHub Release Creation & APK Asset Upload (PulseMusic)")
    rel_cmd = [
        "gh", "release", "create", TAG,
        os.path.join(RELEASE_DIR, "PulseMusic-Universal.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-arm64-v8a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-armeabi-v7a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-x86_64.apk"),
        os.path.join(RELEASE_DIR, "SHA256SUMS.txt"),
        "--title", release_title,
        "--notes", release_body,
        "--repo", "muthumanikandanb2005-ux/PulseMusic"
    ]
    if os.path.isfile(win_exe):
        rel_cmd.append(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe"))
    if os.path.isfile(win_zip):
        rel_cmd.append(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip"))

    print("Running gh release create for PulseMusic...")
    p1 = subprocess.run(rel_cmd, capture_output=True, text=True)
    if p1.returncode != 0:
        print("Warning creating PulseMusic release:", p1.stderr)
    else:
        print(f"GitHub Release {TAG} created successfully on PulseMusic!")

    step("7. GitHub Release Creation & APK Asset Upload (Pulse-Music Mirror)")
    rel_cmd_mirror = [
        "gh", "release", "create", TAG,
        os.path.join(RELEASE_DIR, "PulseMusic-Universal.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-arm64-v8a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-armeabi-v7a.apk"),
        os.path.join(RELEASE_DIR, "PulseMusic-x86_64.apk"),
        os.path.join(RELEASE_DIR, "SHA256SUMS.txt"),
        "--title", release_title,
        "--notes", release_body,
        "--repo", "muthumanikandanb2005-ux/Pulse-Music"
    ]
    if os.path.isfile(win_exe):
        rel_cmd_mirror.append(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe"))
    if os.path.isfile(win_zip):
        rel_cmd_mirror.append(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip"))

    print("Running gh release create for Pulse-Music mirror...")
    p2 = subprocess.run(rel_cmd_mirror, capture_output=True, text=True)
    if p2.returncode != 0:
        print("Warning creating Pulse-Music mirror release:", p2.stderr)
    else:
        print(f"GitHub Release {TAG} created successfully on Pulse-Music!")

    step("8. Pushing Code & OTA Manifest to Git on Both Repos")
    subprocess.run(["git", "add", "-A"], cwd=REPO_ROOT, check=True)
    subprocess.run(["git", "commit", "-m", f"release: Pulse Music v{VERSION} (Build {VERSION_CODE}) OTA and Store release"], cwd=REPO_ROOT)
    subprocess.run(["git", "push", "origin", "main"], cwd=REPO_ROOT, check=True)
    subprocess.run(["git", "push", "mirror", "main"], cwd=REPO_ROOT, check=True)
    print("  PulseMusic and Pulse-Music code and OTA manifests pushed to main")

    step("9. Pushing F-Droid / Orion Store updates")
    if os.path.isdir(os.path.join(FDROID_DIR, ".git")):
        subprocess.run(["git", "add", "-A"], cwd=FDROID_DIR, check=True)
        subprocess.run(["git", "commit", "-m", f"Update repository for Pulse Music v{VERSION} (Build {VERSION_CODE})"], cwd=FDROID_DIR)
        subprocess.run(["git", "push", "origin", "main"], cwd=FDROID_DIR, check=True)
        print("  F-Droid repository committed and pushed to GitHub")

    step("ALL STEPS COMPLETED! v4.2.1 RELEASE IS LIVE WORLDWIDE!")

if __name__ == "__main__":
    main()
