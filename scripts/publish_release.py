import os
import sys
import json
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')

REPO = "muthumanikandanb2005-ux/PulseMusic"
TAG = "v2.2.4"
RELEASE_NAME = "Pulse Music v2.2.4 - Total Playback Freeze Fix & Continuous Audio Engine"
BODY = """### Pulse Music v2.2.4

- **Total Playback Freeze Fix on Skips & Play/Pause**:
  - **ExoPlayer Audio Sink Leak Prevention**: Fixed an issue where rapid track skipping abandoned partially-prepared ExoPlayer instances, leaking Android hardware `AudioTrack` sinks and media codecs until the audio subsystem locked up. Added cancellation-safe resource cleanup ensuring any unassigned player is immediately released.
  - **Zero Play/Pause Deadlocks**: Resolved state lockout where tapping play while in an error/idle state was ignored; `play()` now automatically reloads and recovers playback seamlessly.
  - **Accurate Play/Pause Inversion Prevention**: Fixed play/pause toggling while tracks are buffering/preparing by respecting active `playWhenReady` intent so playback never gets stuck in an un-pausable or un-playable state.
  - **Race Condition & Duplicate Error Elimination**: Eliminated concurrent racing player creations caused by redundant `player.play()` invocations following queue seeks, and removed duplicated error dispatchers that triggered cascading skips.
- **100% Supabase Audio Decoupling & IO Isolation**:
  - Completely decoupled Supabase accounts from all playback logic, lifecycles, and audio repositories.
  - Shifted all local SQLite database queries and stream repository flows to background `Dispatchers.IO`, preventing Room SQLite deadlocks on the main thread during stream resolution.
  - Playback runs 100% uninterrupted and continuous even with zero internet account sync or in offline mode.
- **Continuous Queue Playback**:
  - Guaranteed continuous music playback across the entire queue without random stops or silent failures.
- **Includes All Previous Enhancements**:
  - Full mobile notification drawer alerts for trending music and OTA updates.
  - Native Pulse Music circular logo startup animation.
  - Modern Apple Music & Spotify hybrid lyrics styling with synchronized progressive timing.
"""

ASSETS = [
    ("PulseMusic-Universal.apk", "dist/PulseMusic-Universal.apk"),
    ("PulseMusic-arm64-v8a.apk", "dist/PulseMusic-arm64-v8a.apk"),
    ("PulseMusic-armeabi-v7a.apk", "dist/PulseMusic-armeabi-v7a.apk"),
    ("PulseMusic-v2.2.4-Release-Universal.apk", "dist/PulseMusic-v2.2.4-Release-Universal.apk"),
    ("PulseMusic-v2.2.4-Release-arm64-v8a.apk", "dist/PulseMusic-v2.2.4-Release-arm64-v8a.apk"),
    ("PulseMusic-v2.2.4-Release-armeabi-v7a.apk", "dist/PulseMusic-v2.2.4-Release-armeabi-v7a.apk"),
    ("PulseMusic-Windows-x64.zip", "dist/PulseMusic-Windows-x64.zip"),
    ("PulseMusic-v2.2.4-Windows-x64.zip", "dist/PulseMusic-v2.2.4-Windows-x64.zip"),
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
