import os
import sys
import json
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')

REPO = "muthumanikandanb2005-ux/PulseMusic"
TAG = "v2.2.1"
RELEASE_NAME = "Pulse Music v2.2.1 - App Logo Splash, Continuous Video Playback, Hybrid Lyrics & OTA Update Notifications"
BODY = """### Pulse Music v2.2.1

- **Native App Logo Startup Animation**:
  - Matches the official Pulse Music app logo with gentle ambient radial breathing, smooth glow aura, and refined fade transition.
- **Smooth & Freeze-Free Video Songs Playback**:
  - Fixed video songs playback freezing by upgrading player surface pipeline to hardware-accelerated `TextureView` and preventing period clipping locks in `MergingMediaSource`.
- **Apple Music + Spotify Mixed Design Lyrics**:
  - Premium hybrid lyrics interface featuring large bold active typography (26sp), defocused inactive lines (alpha 0.35, scale 0.96), frosted glass pill containers, and ultra-smooth line & word synchronization.
  - Soft silver romanization and soft sage translations for multilingual clarity.
- **Progressive Playback Timing Fix**:
  - Fixed duration and progress playback calculations in Pulse Player, restoring accurate live track times and interactive playhead scrub thumb.
- **Soft Color Palette Harmonization**:
  - Replaced harsh neon tones across home, settings, and login screens with gentle modern emerald & mint tones (`#10B981` / `#34D399`).
- **Direct Mobile Notification Panel for New Updates**:
  - Integrated immediate notification shade alerts on Android (`pulse_app_update_channel`) as soon as a new OTA update is detected, allowing direct updates from the phone's notification drawer.
"""

ASSETS = [
    ("PulseMusic-Universal.apk", "dist/PulseMusic-Universal.apk"),
    ("PulseMusic-arm64-v8a.apk", "dist/PulseMusic-arm64-v8a.apk"),
    ("PulseMusic-armeabi-v7a.apk", "dist/PulseMusic-armeabi-v7a.apk"),
    ("PulseMusic-v2.2.1-Release-Universal.apk", "dist/PulseMusic-v2.2.1-Release-Universal.apk"),
    ("PulseMusic-v2.2.1-Release-arm64-v8a.apk", "dist/PulseMusic-v2.2.1-Release-arm64-v8a.apk"),
    ("PulseMusic-v2.2.1-Release-armeabi-v7a.apk", "dist/PulseMusic-v2.2.1-Release-armeabi-v7a.apk"),
    ("PulseMusic-Windows-x64.zip", "dist/PulseMusic-Windows-x64.zip"),
    ("PulseMusic-v2.2.1-Windows-x64.zip", "dist/PulseMusic-v2.2.1-Windows-x64.zip"),
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
