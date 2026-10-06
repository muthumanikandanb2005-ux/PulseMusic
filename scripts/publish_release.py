import os
import sys
import json
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')

REPO = "muthumanikandanb2005-ux/PulseMusic"
TAG = "v2.2.0"
RELEASE_NAME = "Pulse Music v2.2.0 - Premium Splash Animation, Kinetic Neon Lyrics & Mobile System Notifications"
BODY = """### Pulse Music v2.2.0

- **Premium Startup Splash Animation**:
  - Pulsating radiant emerald & cyan halo ring with animated equalizer soundwave bars.
  - Glowing **PULSE MUSIC** typography with "PREMIUM AUDIO" badge and smooth fade transition into the app.
- **Pulse Kinetic Neon Lyrics (Ultra-Synced)**:
  - All-new futuristic lyrics style featuring glowing frosted emerald-cyan pill container (`#00E676` to `#00F5D4`).
  - Active line expands with kinetic spring scaling and displays a glowing pulse indicator dot.
  - Interactive click-to-seek, smooth vertical center-locking, and support for word-by-word and line-by-line sync.
- **Lyrics Language Consistency Fix**:
  - Solved issue where some songs displayed lyrics in foreign languages by prioritizing native audio language and human-created captions over auto-generated speech recognition (`a.`) tracks.
  - Preserves original song lyrics as primary text, with clean phonetic romanization and translations as subtext.
- **Music Playback Time Display**:
  - High-visibility monospace `Time 01:25 / 03:45` badge on the player with elapsed and remaining time.
- **Mobile Push & System Notification Panel**:
  - Integrated native Android notification drawer channels:
    - `pulse_trending_music_channel`: System notifications for trending tracks, charts, and new artist releases.
    - `pulse_app_update_channel`: Direct in-shade alerts when a new OTA version is available or ready to install.
- **Complete Removal of BitChord Branding**:
  - Replaced all remaining legacy labels with Pulse Music and Pulse Dynamic Neon across player styles, settings, and home banners.
- **Unified Profile Hub with Google Sign-In**:
  - Instant access to Listening History, Liked Songs, Playlists, Supabase Cloud Sync, and Profile Demographics.
"""

ASSETS = [
    ("PulseMusic-Universal.apk", "dist/PulseMusic-Universal.apk"),
    ("PulseMusic-arm64-v8a.apk", "dist/PulseMusic-arm64-v8a.apk"),
    ("PulseMusic-armeabi-v7a.apk", "dist/PulseMusic-armeabi-v7a.apk"),
    ("PulseMusic-v2.2.0-Release-Universal.apk", "dist/PulseMusic-v2.2.0-Release-Universal.apk"),
    ("PulseMusic-v2.2.0-Release-arm64-v8a.apk", "dist/PulseMusic-v2.2.0-Release-arm64-v8a.apk"),
    ("PulseMusic-v2.2.0-Release-armeabi-v7a.apk", "dist/PulseMusic-v2.2.0-Release-armeabi-v7a.apk"),
    ("PulseMusic-Windows-x64.zip", "dist/PulseMusic-Windows-x64.zip"),
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
