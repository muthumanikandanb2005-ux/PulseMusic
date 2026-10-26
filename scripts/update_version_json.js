const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '..', 'version.json');
const v = JSON.parse(fs.readFileSync(filePath, 'utf8'));

v.name = 'Pulse Music v3.2.9 - Zero-Freeze Playback, Instant Desktop Transitions & Universal com.pulse.music';
v.body = `## Pulse Music v3.2.9 - Zero-Freeze Playback, Instant Desktop Transitions & Universal com.pulse.music

### ⚡ Zero-Freeze Continuous Playback (Android)
- **Eliminated 1-Song Freeze**: Decoupled user drag interactions from programmatic pager animation in NowPlayingScreen, ensuring automatic track advancement never triggers infinite re-entrant seek cycles or main UI lockups.
- **Continuous Zero-Lock Engine**: Non-blocking asynchronous database flows replace all blocking SQLite terminal operators across playback handlers, queue workers, and background receivers.

### 🚀 Instant Desktop Transitions (Windows)
- **Instant Track & Artwork Switching**: Changing tracks or clicking Next/Previous on Windows now broadcasts metadata and album artwork synchronously on seek entry, delivering instant 0ms visual updates.
- **High-Speed Artwork Transitions**: Tuned crossfade latency in MiniPlayer for rapid, seamless artwork rendering.

### 🛡️ Listen Healthy & Ear Protection
- **WHO 85dB Volume Limiter**: Protects long-term hearing by capping headphone output at 75%.
- **Ear Break Reminders & Exposure Monitor**: Real-time listening session tracking with customizable rest intervals.

### 🧭 Smart Discovery Mix
- **Discovery Mix Slider**: Dynamic ratio balance between familiar library tracks and algorithmic recommendations.
- **Quick Picks Discovery Toggle**: Live Discovery ratio badge on Home Quick Picks header.

### 🎨 Visual & Lyrics Polish
- **Dynamic Ambient Light**: Ambient dynamic light harmonized with high-resolution album artwork colors.
- **Multilingual Grapheme Cluster Light**: Syllable and letter-level lyrics illumination across global scripts (Tamil, Hindi, Japanese, Korean, Arabic, Latin).
- **Theme Engine**: Instant switching between Dark AMOLED (#000000), Pure Light, System Default, and Pastel Pearl Lavender.
`;

fs.writeFileSync(filePath, JSON.stringify(v, null, 2) + '\n', 'utf8');
console.log('version.json updated successfully');
