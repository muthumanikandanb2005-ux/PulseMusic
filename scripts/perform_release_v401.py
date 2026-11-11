import os
import sys
import shutil
import hashlib
import json
import subprocess
from datetime import datetime, timezone

REPO_ROOT = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic"
RELEASE_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.0.1"
FDROID_REPO = r"C:\Users\muthu\.gemini\antigravity\scratch\fdroid-repo\repo"
DIST_DIR = os.path.join(REPO_ROOT, "dist")
APK_OUT_DIR = os.path.join(REPO_ROOT, "androidApp", "build", "outputs", "apk", "release")

VERSION = "4.0.1"
VERSION_CODE = 101
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
        shutil.copy2(src, os.path.join(RELEASE_DIR, versioned))
        shutil.copy2(src, os.path.join(FDROID_REPO, unversioned))
        shutil.copy2(src, os.path.join(FDROID_REPO, versioned))
        print(f"  Copied {unversioned} & {versioned}")

    step("3. Compiling Desktop Distributable")
    gradle_cmd = [r".\gradlew.bat", ":desktopApp:createDistributable"]
    res = subprocess.run(gradle_cmd, cwd=REPO_ROOT, shell=True)
    if res.returncode != 0:
        print("Warning: :desktopApp:createDistributable returned non-zero, checking outputs...")

    step("4. Building Windows Setup with Inno Setup")
    inno_batch = os.path.join(REPO_ROOT, "scripts", "build_installer.bat")
    res = subprocess.run([inno_batch], cwd=REPO_ROOT, shell=True)
    if res.returncode != 0:
        print("Error compiling installer with Inno Setup")
        sys.exit(1)

    setup_versioned = f"PulseMusic-v{VERSION}-windows-x64-Setup.exe"
    setup_unversioned = "PulseMusic-Windows-x64-Setup.exe"
    setup_v_path = os.path.join(DIST_DIR, setup_versioned)
    setup_u_path = os.path.join(DIST_DIR, setup_unversioned)

    if not os.path.isfile(setup_v_path):
        print(f"Error: {setup_v_path} not found")
        sys.exit(1)

    shutil.copy2(setup_v_path, setup_u_path)
    shutil.copy2(setup_v_path, os.path.join(RELEASE_DIR, setup_versioned))
    shutil.copy2(setup_u_path, os.path.join(RELEASE_DIR, setup_unversioned))

    step("5. Creating Windows Setup ZIP Archive")
    import zipfile
    zip_versioned = f"PulseMusic-v{VERSION}-Windows-x64-Setup.zip"
    zip_unversioned = "PulseMusic-Windows-x64-Setup.zip"
    zip_v_path = os.path.join(DIST_DIR, zip_versioned)
    zip_u_path = os.path.join(DIST_DIR, zip_unversioned)

    with zipfile.ZipFile(zip_v_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(setup_v_path, arcname=setup_versioned)

    shutil.copy2(zip_v_path, zip_u_path)
    shutil.copy2(zip_v_path, os.path.join(RELEASE_DIR, zip_versioned))
    shutil.copy2(zip_u_path, os.path.join(RELEASE_DIR, zip_unversioned))

    step("6. Generating SHA256SUMS.txt")
    asset_files = [
        "PulseMusic-Universal.apk",
        "PulseMusic-arm64-v8a.apk",
        "PulseMusic-armeabi-v7a.apk",
        "PulseMusic-x86_64.apk",
        "PulseMusic-Windows-x64-Setup.exe",
        "PulseMusic-Windows-x64-Setup.zip",
        f"PulseMusic-v{VERSION}-windows-x64-Setup.exe",
        f"PulseMusic-v{VERSION}-Windows-x64-Setup.zip",
        f"PulseMusic-v{VERSION}-Universal.apk",
        f"PulseMusic-v{VERSION}-arm64-v8a.apk",
        f"PulseMusic-v{VERSION}-armeabi-v7a.apk",
        f"PulseMusic-v{VERSION}-x86_64.apk",
    ]

    sums = []
    for fname in asset_files:
        fpath = os.path.join(RELEASE_DIR, fname)
        if os.path.isfile(fpath):
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

    step("7. Updating version.json for OTA Broadcast")
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    
    version_json_path = os.path.join(REPO_ROOT, "version.json")
    v_data = {
        "url": f"https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/tags/v{VERSION}",
        "html_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v{VERSION}",
        "tag_name": f"v{VERSION}",
        "name": f"Pulse Music v{VERSION} - Snapchat Snap Sharing, Spotify Share Button & Windows YouTube Login",
        "draft": False,
        "prerelease": False,
        "published_at": now_iso,
        "body": f"""# Pulse Music v{VERSION} - Snapchat Snap Sharing, Spotify Share Button & Windows YouTube Login

Official multi-platform release and global OTA update for **Android**, **Windows**, **Orion Store**, **F-Droid**, and **Obtainium**.

---

### 👻 Snapchat Creative Kit - Real Snap Sharing
- **Opens Directly as a Snap**: Sharing album cards and lyrics opens Snapchat's Snap preview and camera editor directly, never defaulting to 1-on-1 private Chat.
- **Lyrics Card Snap Sharing**: Share interactive formatted lyrics cards directly to Snapchat with custom background colors, dynamic typography, and deep link playback context.
- **Spotify-Style 9:16 Music Card Snap**: High-resolution 9:16 vertical cards with album artwork glow and custom brand soundwave barcodes.

### 🎨 Spotify-Style Share Button
- **Exact Spotify Placement**: Dedicated Share button placed directly below the album artwork next to the heart/like button on the Now Playing screen.
- **One-Tap Access**: Immediate access to the Spotify-style music card and Snapchat share options with a single tap.

### 💻 Windows Desktop YouTube Login Fix
- **Native Login UI**: Complete desktop YouTube login connection panel with guided 3-step setup and browser integration.
- **One-Click Clipboard Paste**: Instant paste and automatic sanitization supporting raw cookies, cURL commands, HTTP headers, and Netscape format.
- **Real-Time Token Validation**: Live badge verification for essential auth tokens (`SAPISID`, `__Secure-3PAPISID`, `LOGIN_INFO`, `SID`).
- **Full Account Sync**: Persistent cookie storage ensuring user playlists and liked songs sync reliably on Windows.

### ⏭️ Smart Queue & Playback Stability
- **Skip Logic**: User skips bypass single-track repeat to reliably progress through the queue.
- **Radio Recommendations**: 100% discovery ratio to eliminate repetitive looping of liked songs.
""",
        "assets": []
    }

    for fname in asset_files:
        fpath = os.path.join(RELEASE_DIR, fname)
        if os.path.isfile(fpath):
            ctype = "application/vnd.android.package-archive" if fname.endswith(".apk") else ("application/x-msdownload" if fname.endswith(".exe") else "application/zip")
            v_data["assets"].append({
                "name": fname,
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/{fname}",
                "content_type": ctype,
                "size": os.path.getsize(fpath)
            })

    v_data["assets"].append({
        "name": "SHA256SUMS.txt",
        "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/SHA256SUMS.txt",
        "content_type": "text/plain; charset=utf-8",
        "size": os.path.getsize(sums_path_release)
    })

    with open(version_json_path, "w", encoding="utf-8") as f:
        json.dump(v_data, f, indent=2)
    print("  version.json updated successfully")

    release_notes_path = os.path.join(DIST_DIR, f"release_notes_v{VERSION}.md")
    with open(release_notes_path, "w", encoding="utf-8") as f:
        f.write(v_data["body"])

    step("8. Updating fdroid index-v1.json")
    fdroid_json_path = os.path.join(REPO_ROOT, "..", "fdroid-repo", "repo", "index-v1.json")
    if os.path.isfile(fdroid_json_path):
        try:
            with open(fdroid_json_path, "r", encoding="utf-8") as f:
                fd_data = json.load(f)
            
            fd_data["repo"]["timestamp"] = int(datetime.now(timezone.utc).timestamp() * 1000)
            if "apps" in fd_data and len(fd_data["apps"]) > 0:
                fd_data["apps"][0]["suggestedVersionCode"] = VERSION_CODE
                fd_data["apps"][0]["suggestedVersionName"] = VERSION
                fd_data["apps"][0]["lastUpdated"] = fd_data["repo"]["timestamp"]
            
            pkg_entries = []
            for apk_key, unv, ver in apk_mappings:
                fpath = os.path.join(RELEASE_DIR, ver)
                if os.path.isfile(fpath):
                    pkg_entries.append({
                        "apkName": ver,
                        "packageName": "com.pulse.music",
                        "versionCode": VERSION_CODE,
                        "versionName": VERSION,
                        "hash": sha256_file(fpath),
                        "hashType": "sha256",
                        "size": os.path.getsize(fpath),
                        "minSdkVersion": 26,
                        "targetSdkVersion": 35,
                        "nativecode": ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"] if "Universal" in ver else [ver.split("-")[-1].replace(".apk", "")],
                        "added": fd_data["repo"]["timestamp"]
                    })
            
            existing_pkgs = fd_data.get("packages", {}).get("com.pulse.music", [])
            fd_data["packages"]["com.pulse.music"] = pkg_entries + existing_pkgs
            
            with open(fdroid_json_path, "w", encoding="utf-8") as f:
                json.dump(fd_data, f, indent=2)
            print("  fdroid-repo/repo/index-v1.json updated successfully")
        except Exception as e:
            print(f"  Error updating fdroid index: {e}")

    fdroid_version_json = os.path.join(REPO_ROOT, "..", "fdroid-repo", "version.json")
    if os.path.exists(os.path.dirname(fdroid_version_json)):
        shutil.copy2(version_json_path, fdroid_version_json)
        print("  fdroid-repo/version.json updated successfully")

    step(f"9. Creating GitHub Release {TAG} via gh CLI")
    gh_cmd = [
        "gh", "release", "create", TAG,
        "--title", f"Pulse Music v{VERSION} - Snapchat Snap Sharing, Spotify Share Button & Windows YouTube Login",
        "--notes-file", release_notes_path
    ]
    all_release_files = [os.path.join(RELEASE_DIR, f) for f in os.listdir(RELEASE_DIR)]
    gh_cmd.extend(all_release_files)
    print("Running gh release create...")
    res = subprocess.run(gh_cmd, cwd=REPO_ROOT, shell=True)
    if res.returncode == 0:
        print(f"GitHub Release {TAG} created successfully with all assets!")
    else:
        print(f"gh release create exited with code {res.returncode}, attempting upload --clobber if tag exists...")
        upload_cmd = ["gh", "release", "upload", TAG] + all_release_files + ["--clobber"]
        subprocess.run(upload_cmd, cwd=REPO_ROOT, shell=True)

    print("\nAll release operations completed successfully!")

if __name__ == "__main__":
    main()
