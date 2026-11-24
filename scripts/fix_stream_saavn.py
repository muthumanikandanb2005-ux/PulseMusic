path = r"C:\Users\muthu\.gemini\antigravity\scratch\PulseMusic\core\data\src\commonMain\kotlin\com\manikandan\data\repository\StreamRepositoryImpl.kt"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# Fix the artist and durationSeconds references
text = text.replace("song?.artists?.firstOrNull()?.name", "song?.artistName?.firstOrNull()")
text = text.replace("response.videoDetails?.lengthSeconds?.toInt() ?: song?.duration", "response.videoDetails?.lengthSeconds?.toIntOrNull() ?: song?.durationSeconds")
text = text.replace("lengthSeconds = song.duration,", "lengthSeconds = song.durationSeconds,")

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("StreamRepositoryImpl.kt properties corrected!")
