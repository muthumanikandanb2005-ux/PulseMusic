import os
import sys
import json
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')

REPO = "muthumanikandanb2005-ux/PulseMusic"
TAG = "v3.0.0"
RELEASE_NAME = "Pulse Music v3.0.0 - Pastel Pearl Lavender Theme, Mobile Landscape Player, Windows DataStore Fix & Cross-Device Account Sync"
BODY = """### Pulse Music v3.0.0 (OTA Major Update)

- 🎨 **Pastel Pearl Lavender Theme**:
  - Brand-new elegant Pastel Pearl Lavender theme inspired by soft lavender, violet, and creamy pearl tones.
  - Dedicated toggle switch in **Settings -> Appearance & Themes** with instant persistence across app restarts.
  - Carefully tuned high contrast for full accessibility in both Light and Dark modes.

- 📱 **Mobile Landscape Playback Enhancements**:
  - Redesigned landscape playback interface specifically optimized for phones.
  - Perfectly aligned side-by-side layout: prominent square album poster on the left with breathable padding.
  - Full playback controls on the right: previous track, play/pause toggle, next track, repeat, shuffle, and progress scrubber properly spaced and aligned.

- 🔄 **Cross-Device Account Sync (Android & Windows)**:
  - Seamless cloud synchronization across all devices connected to the same account via Supabase.
  - Automatically syncs **Liked Songs**, **Custom Playlists**, **Listening History**, and **Active Playback State / Position**.
  - Continue listening right where you left off when switching between your phone and PC.

- ⏪ **Track Playback Reset Fix**:
  - Fixed an issue where searching and playing a new song resumed from the previous song's paused position instead of starting from the beginning.
  - New songs now reliably start playing from 0:00 (the very beginning) across both Android (Media3/ExoPlayer) and Desktop (MPV).

- 🪟 **Windows DataStore Stability**:
  - Fixed `java.io.IOException: Unable to rename settings.preferences_pb.tmp` crash on Windows.
  - Implemented thread-safe process-wide singleton DataStore instance with automatic cleanup of orphaned temporary preference files.
"""

ASSETS = [
    ("PulseMusic-Universal.apk", "dist/PulseMusic-Universal.apk"),
    ("PulseMusic-arm64-v8a.apk", "dist/PulseMusic-arm64-v8a.apk"),
    ("PulseMusic-armeabi-v7a.apk", "dist/PulseMusic-armeabi-v7a.apk"),
    ("PulseMusic-v3.0.0-Release-Universal.apk", "dist/PulseMusic-v3.0.0-Release-Universal.apk"),
    ("PulseMusic-v3.0.0-Release-arm64-v8a.apk", "dist/PulseMusic-v3.0.0-Release-arm64-v8a.apk"),
    ("PulseMusic-v3.0.0-Release-armeabi-v7a.apk", "dist/PulseMusic-v3.0.0-Release-armeabi-v7a.apk"),
    ("PulseMusic-Windows-x64-Setup.exe", "dist/PulseMusic-Windows-x64-Setup.exe"),
    ("PulseMusic-v3.0.0-Windows-x64-Setup.exe", "dist/PulseMusic-v3.0.0-Windows-x64-Setup.exe"),
    ("PulseMusic-Windows-x64.zip", "dist/PulseMusic-Windows-x64.zip"),
    ("PulseMusic-v3.0.0-Windows-x64.zip", "dist/PulseMusic-v3.0.0-Windows-x64.zip"),
    ("SHA256SUMS.txt", "dist/SHA256SUMS.txt"),
]

def main():
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token and os.path.exists(".env"):
        with open(".env") as f:
            for line in f:
                if line.startswith("GITHUB_TOKEN="):
                    token = line.split("=", 1)[1].strip().strip('"').strip("'")
                    break

    if not token:
        try:
            import subprocess
            token = subprocess.check_output(["gh", "auth", "token"]).decode("utf-8").strip()
        except Exception:
            pass

    if not token:
        print("ERROR: GITHUB_TOKEN environment variable is not set and gh auth token could not be retrieved.")
        sys.exit(1)

    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "PulseMusicReleaseScript"
    }

    print(f"Creating release {TAG} on {REPO}...")
    create_url = f"https://api.github.com/repos/{REPO}/releases"
    payload = {
        "tag_name": TAG,
        "target_commitish": "main",
        "name": RELEASE_NAME,
        "body": BODY,
        "draft": False,
        "prerelease": False
    }

    req = urllib.request.Request(create_url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            release_id = data["id"]
            upload_url_template = data["upload_url"] # e.g. https://uploads.github.com/repos/.../releases/{id}/assets{?name,label}
            upload_base = upload_url_template.split("{")[0]
            print(f"✓ Release created successfully (ID: {release_id})")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        if e.code == 422 and "already_exists" in err_msg:
            print(f"Release {TAG} already exists. Fetching existing release...")
            get_req = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases/tags/{TAG}", headers=headers)
            with urllib.request.urlopen(get_req) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                release_id = data["id"]
                upload_base = data["upload_url"].split("{")[0]
        else:
            print(f"Failed to create release: {e.code} {err_msg}")
            sys.exit(1)

    # Query existing assets on this release and delete duplicates to allow clean re-upload
    assets_req = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases/{release_id}/assets", headers=headers)
    existing_assets = {}
    try:
        with urllib.request.urlopen(assets_req) as resp:
            for item in json.loads(resp.read().decode("utf-8")):
                existing_assets[item["name"]] = item["id"]
    except Exception as e:
        print(f"Note: Could not list existing assets: {e}")

    for asset_name, asset_path in ASSETS:
        if not os.path.exists(asset_path):
            print(f"Skipping {asset_name}: {asset_path} does not exist.")
            continue

        if asset_name in existing_assets:
            print(f"Deleting existing asset {asset_name} (ID: {existing_assets[asset_name]})...")
            del_req = urllib.request.Request(
                f"https://api.github.com/repos/{REPO}/releases/assets/{existing_assets[asset_name]}",
                headers=headers,
                method="DELETE"
            )
            try:
                with urllib.request.urlopen(del_req) as resp:
                    print(f"✓ Removed old {asset_name}")
            except Exception as e:
                print(f"Warning: Failed to delete old asset {asset_name}: {e}")

        file_size = os.path.getsize(asset_path)
        print(f"Uploading {asset_name} ({file_size / (1024*1024):.1f} MB)...")
        upload_url = f"{upload_base}?name={urllib.parse.quote(asset_name)}"
        upload_headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/vnd.android.package-archive",
            "User-Agent": "PulseMusicReleaseScript",
            "Content-Length": str(file_size)
        }

        with open(asset_path, "rb") as f:
            upload_req = urllib.request.Request(upload_url, data=f, headers=upload_headers, method="POST")
            try:
                with urllib.request.urlopen(upload_req) as resp:
                    print(f"✓ Uploaded {asset_name}")
            except urllib.error.HTTPError as e:
                err = e.read().decode("utf-8")
                print(f"Warning: Failed to upload {asset_name}: {e.code} {err}")

    print("\n✓ OTA Release published successfully on GitHub!")
    print(f"View release: https://github.com/{REPO}/releases/tag/{TAG}")

if __name__ == "__main__":
    main()
