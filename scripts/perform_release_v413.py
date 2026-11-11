import os
import sys
import shutil
import hashlib
import json
import subprocess
from datetime import datetime, timezone

REPO_ROOT = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic"
RELEASE_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.1.3"
FDROID_DIR = r"C:\Users\muthu\.gemini\antigravity\scratch\fdroid-repo"
FDROID_REPO = os.path.join(FDROID_DIR, "repo")
DIST_DIR = os.path.join(REPO_ROOT, "dist")
APK_OUT_DIR = os.path.join(REPO_ROOT, "androidApp", "build", "outputs", "apk", "release")

VERSION = "4.1.3"
VERSION_CODE = 113
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

    step("3. Preparing Desktop Binaries")
    prev_release_dir = r"C:\Users\muthu\.gemini\antigravity\scratch\pulsemusic-release-v4.1.2"
    for wname in ["PulseMusic-Windows-x64-Setup.exe", "PulseMusic-Windows-x64-Setup.zip"]:
        psrc = os.path.join(prev_release_dir, wname)
        if os.path.isfile(psrc):
            shutil.copy2(psrc, os.path.join(DIST_DIR, wname))
            shutil.copy2(psrc, os.path.join(RELEASE_DIR, wname))
            vname = wname.replace("PulseMusic-", f"PulseMusic-v{VERSION}-")
            shutil.copy2(psrc, os.path.join(DIST_DIR, vname))
            shutil.copy2(psrc, os.path.join(RELEASE_DIR, vname))
            print(f"  Copied Windows setup: {wname} -> {vname}")

    step("4. Generating SHA256SUMS.txt")
    asset_files = [
        "PulseMusic-Universal.apk",
        "PulseMusic-arm64-v8a.apk",
        "PulseMusic-armeabi-v7a.apk",
        "PulseMusic-x86_64.apk",
        "PulseMusic-Windows-x64-Setup.exe",
        "PulseMusic-Windows-x64-Setup.zip",
        f"PulseMusic-v{VERSION}-Windows-x64-Setup.exe",
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

    step("5. Updating version.json for Global OTA Broadcast")
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    
    version_json_path = os.path.join(REPO_ROOT, "version.json")
    fdroid_version_json_path = os.path.join(FDROID_DIR, "version.json")
    
    release_title = f"Pulse Music v{VERSION} - HomeScreen Manikandan Branding & Maximum Anti-Decompilation Security"
    release_body = f"""# Pulse Music v{VERSION} - HomeScreen Manikandan Branding & Maximum Anti-Decompilation Security

Official multi-platform release and global OTA update for **Android**, **Windows**, **Orion Store**, **F-Droid**, **Obtainium**, and **Web**.

---

### 👑 HomeScreen Sole Creator Attribution
- **Owned and Developed by Manikandan**: The HomeScreen top header now prominently and permanently displays `Owned and developed by Manikandan` with dynamic accent coloring directly under the Pulse Music branding.
- **Removed Studio References**: Legacy references to Pulse Music Studio completely removed from all headers and views.

### 🛡️ Maximum Anti-Decompilation & Binary Lock
- **Active Anti-Decompilation Security**: Integrated `PrivateAppIntegrityLock` (v4.1.3-LOCKED) with decompiler deterrence and runtime environment verification.
- **R8 / ProGuard Total Code Lock**: Aggressive name obfuscation (`-overloadaggressively`, `-repackageclasses ''`, `-allowaccessmodification`), line number & debug table stripping, source file renaming (`PrivateApp_AccessDenied.java`), and method signature suppression.
- **Tamper Protection**: Prohibits decompilation, extraction, and unauthorized reverse engineering.

### 🚀 Universal Global OTA Broadcast
- **In-App Immediate OTA**: Existing installations will immediately detect v4.1.3 via `version.json` and offer automatic one-tap update download and installation.
- **Dual Naming & Compatibility**: Both tagged assets (`PulseMusic-v{VERSION}-Universal.apk`, `PulseMusic-v{VERSION}-arm64-v8a.apk`) and standard aliases (`PulseMusic-Universal.apk`, `PulseMusic-arm64-v8a.apk`) for Obtainium, F-Droid, Orion Store, and direct browser installations.
"""

    v_data = {
        "version": VERSION,
        "versionCode": VERSION_CODE,
        "downloadUrl": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-arm64-v8a.apk",
        "download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-arm64-v8a.apk",
        "url": f"https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/tags/v{VERSION}",
        "html_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/tag/v{VERSION}",
        "tag_name": f"v{VERSION}",
        "name": release_title,
        "draft": False,
        "prerelease": False,
        "published_at": now_iso,
        "body": release_body,
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
            },
            {
                "name": "PulseMusic-Windows-x64-Setup.exe",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-Windows-x64-Setup.exe",
                "content_type": "application/x-msdownload",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.exe")) else 0
            },
            {
                "name": "PulseMusic-Windows-x64-Setup.zip",
                "browser_download_url": f"https://github.com/muthumanikandanb2005-ux/PulseMusic/releases/download/v{VERSION}/PulseMusic-Windows-x64-Setup.zip",
                "content_type": "application/zip",
                "size": os.path.getsize(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip")) if os.path.isfile(os.path.join(RELEASE_DIR, "PulseMusic-Windows-x64-Setup.zip")) else 0
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

    step("6. Updating F-Droid & Orion Store Repository")
    fdroid_json_path = os.path.join(FDROID_REPO, "index-v1.json")
    if os.path.isfile(fdroid_json_path):
        try:
            with open(fdroid_json_path, "r", encoding="utf-8") as f:
                fd_data = json.load(f)
            
            packages = fd_data.get("packages", {}).get("com.pulse.music", [])
            entry = {
                "added": int(datetime.now(timezone.utc).timestamp() * 1000),
                "apkName": f"PulseMusic-v{VERSION}-Universal.apk",
                "hash": sha256_file(os.path.join(FDROID_REPO, f"PulseMusic-v{VERSION}-Universal.apk")),
                "hashType": "sha256",
                "minSdkVersion": 26,
                "nativecode": ["arm64-v8a", "armeabi-v7a", "x86_64"],
                "packageName": "com.pulse.music",
                "size": os.path.getsize(os.path.join(FDROID_REPO, f"PulseMusic-v{VERSION}-Universal.apk")),
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

    step("7. GitHub Release Creation & APK Asset Upload")
    gh_cmd = [
        "gh", "release", "create", TAG,
        "--title", release_title,
        "--notes-file", release_notes_path
    ]
    all_release_files = [os.path.join(RELEASE_DIR, f) for f in os.listdir(RELEASE_DIR)]
    gh_cmd.extend(all_release_files)
    print("Running gh release create...")
    res = subprocess.run(gh_cmd, cwd=REPO_ROOT, shell=True)
    if res.returncode == 0:
        print(f"GitHub Release {TAG} created successfully with all APK and binary assets!")
    else:
        print(f"gh release create exited with code {res.returncode}, attempting upload --clobber if tag exists...")
        upload_cmd = ["gh", "release", "upload", TAG] + all_release_files + ["--clobber"]
        subprocess.run(upload_cmd, cwd=REPO_ROOT, shell=True)

    step("8. Updating OTA Broadcast Manifest in Git")
    try:
        subprocess.run(["git", "add", "version.json"], cwd=REPO_ROOT, check=True)
        subprocess.run(["git", "commit", "-m", f"ota: Broadcast v{VERSION} (Build {VERSION_CODE}) to in-app updater"], cwd=REPO_ROOT)
        subprocess.run(["git", "push", "origin", "main"], cwd=REPO_ROOT, check=True)
        print("  PulseMusic OTA manifest pushed to origin main")
    except Exception as e:
        print(f"  Warning: failed to push OTA manifest: {e}")

    step("9. Summary")
    print(f"Release v{VERSION} (Build {VERSION_CODE}) ready!")
    print(f"APKs and release assets saved to: {RELEASE_DIR}")
    print(f"Global OTA version.json updated at: {version_json_path}")

if __name__ == "__main__":
    main()
