const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '..', 'version.json');
const v = JSON.parse(fs.readFileSync(filePath, 'utf8'));

v.name = 'Pulse Music v3.2.7 - Redesigned AMOLED Settings, YouTube Sign In, Windows Shortcuts & Ambient Lighting';
v.body = `## Pulse Music v3.2.7 - Redesigned AMOLED Settings, YouTube Sign In, Windows Shortcuts & Ambient Lighting

### ⚙️ Redesigned AMOLED Settings
- **13 AMOLED Card Groups**: Complete visual parity with pure dark #000000 background, 18dp rounded cards (#161618), and clean border accents (#222226).
- **High-Contrast Custom Switches**: Solid white track with black thumb when ON; dark charcoal with grey thumb when OFF.
- **Segmented Control Buttons**: Segmented pills for Audio Output precision (16-bit PCM | 32-bit float).
- **Real-Time Settings Search**: Instant live filtering across all settings categories and options.

### 🔴 YouTube Sign In
- **Prominent Account Row**: Prominently placed in the first card of Settings.
- **Account State & Switcher**: Shows active channel name with green ACTIVE badge; tap to switch accounts or sign out.
- **Developer Login**: Developer/cookie manual sign-in bottom sheet supported.

### ⌨️ Windows Desktop Keyboard Shortcuts
- **F11**: Toggle Fullscreen
- **Space**: Play / Pause
- **Ctrl + Left / Right**: Previous / Next Track
- **Left / Right**: Seek 5s backward / forward
- **Up / Down**: Volume control (+5% / -5%)
- **M**: Mute / Unmute
- **Ctrl + M**: Toggle Miniplayer
- **Esc**: Close fullscreen player / overlay

### 🎨 Visual & Playback Polish
- **Balanced Ambient Light**: Ambient dynamic light rebalanced with a protective text scrim behind the seekbar and time labels so elapsed time and duration are always crisp and legible.
- **Material Expressive 3 Buttons**: Modern M3 Expressive play/pause, next, and previous buttons with glowing halos removed.
- **Compact Subtitle Lyrics**: Reduced lyrics font size below album art (11.5sp) with bullet dot separator removed.
- **Anti-Blur Artwork**: Enforced high-resolution 1200x1200px artwork resolution (=w1200-h1200).

### 🚀 Performance & Stability
- **Instant Launch**: Multi-stage async background initialization on Android and optimized thread pooling on Windows for rapid launch with zero errors.
- **Rock-Solid Reliability**: Resolved SQLite UNIQUE constraint issues (1555), orientation transitions on Android 16 (SDK 36), and background thread Toast errors.
- **Official Publisher**: Signed by Pulse Music Studio with automated certificate trust and sideloading scripts (install.bat & install.ps1).
`;

fs.writeFileSync(filePath, JSON.stringify(v, null, 2) + '\n', 'utf8');
console.log('version.json updated successfully');
