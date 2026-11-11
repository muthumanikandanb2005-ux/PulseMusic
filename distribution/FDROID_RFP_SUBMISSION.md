# Official F-Droid Request for Packaging (RFP) & Merge Request

### Option A: Open an RFP (Request for Packaging) Issue on GitLab
1. Go to: **[F-Droid RFP Issue Tracker](https://gitlab.com/fdroid/rfp/-/issues/new)**
2. Set the Title to: `RFP: Pulse Music (com.pulse.music)`
3. Copy and paste the template below:

```markdown
### Application Details

- **Name**: Pulse Music
- **Package Name**: com.pulse.music
- **License**: GPL-3.0-or-later
- **Summary**: Modern music player streaming YouTube Music with synced lyrics & zero freezes
- **Source Code**: https://github.com/muthumanikandanb2005-ux/PulseMusic
- **Issue Tracker**: https://github.com/muthumanikandanb2005-ux/PulseMusic/issues
- **Releases**: https://github.com/muthumanikandanb2005-ux/PulseMusic/releases
- **Categories**: Multimedia, Audio

### Description
Pulse Music is a full-featured open-source music streaming application featuring ad-free YouTube Music background streaming, real-time Apple Music and Spotify hybrid styled synchronized lyrics, robust continuous playback engine, equalizer, and offline caching.

### Build details
- Built using Compose Multiplatform / Kotlin Multiplatform.
- Android application directory: `androidApp/`
- Build recipe: `distribution/metadata/com.pulse.music.yml` (already generated and included in repo).
```

---

### Option B: Merge Request to `fdroiddata`
1. Fork **[F-Droid Data](https://gitlab.com/fdroid/fdroiddata)** on GitLab.
2. Add the file `metadata/com.pulse.music.yml` from `distribution/metadata/com.pulse.music.yml`.
3. Open a Merge Request with title: `New app: Pulse Music` against `master`.
