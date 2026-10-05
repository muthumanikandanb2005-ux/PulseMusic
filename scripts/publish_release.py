import os
import sys
import json
import urllib.request
import urllib.parse

REPO = "muthumanikandanb2005-ux/PulseMusic"
TAG = "v2.1.7"
RELEASE_NAME = "Pulse Music v2.1.7 - BitChord UI & OTA Update Engine"
BODY = """### Pulse Music v2.1.7

- **BitChord Music Player UI**: Pure AMOLED black interface, isolated hairline capsule scrubber, and responsive breathing album artwork.
- **Audio Lag Fix**: Fast-start buffer (500ms initial playback latency) and capped memory buffer to eliminate stutters.
- **Package Conflict Resolution**: Completely isolated all ContentProviders under app namespace; fixed conflicting authorities.
- **OTA Updates**: Automatic in-app background update engine wired to GitHub Releases.
- **R8 Release Optimization**: Silky smooth 60/120 FPS performance on low-end and high-end Android devices.
"""

ASSETS = [
    ("PulseMusic-Universal.apk", "dist/PulseMusic-Universal.apk"),
    ("PulseMusic-arm64-v8a.apk", "dist/PulseMusic-arm64-v8a.apk"),
    ("PulseMusic-v2.1.7-Release-Universal.apk", "dist/PulseMusic-v2.1.7-Release-Universal.apk"),
    ("PulseMusic-v2.1.7-Release-arm64-v8a.apk", "dist/PulseMusic-v2.1.7-Release-arm64-v8a.apk"),
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
        print("ERROR: GITHUB_TOKEN environment variable is not set.")
        print("Please set your GitHub Personal Access Token (classic or fine-grained with 'repo' scope):")
        print("PowerShell: $env:GITHUB_TOKEN = 'ghp_your_token_here'; python scripts/publish_release.py")
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
        "target_commitish": "dev",
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

    for asset_name, asset_path in ASSETS:
        if not os.path.exists(asset_path):
            print(f"Skipping {asset_name}: {asset_path} does not exist.")
            continue

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
