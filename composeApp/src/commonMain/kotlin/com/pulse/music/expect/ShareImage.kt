package com.pulse.music.expect

/**
 * Writes [bytes] somewhere the user can find it later: the system gallery on Android, the
 * downloads folder on Desktop.
 *
 * Returns false rather than throwing, because every caller is a button press — the UI has to say
 * something either way, and an exception crossing back into a click handler would take the app
 * down instead.
 */
expect suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean

/**
 * Hands [bytes] to whatever the platform uses to pass a file to another app.
 *
 * Android opens the system share sheet. Desktop has no such thing, so it writes the file out and
 * reveals it in the file manager — the closest honest equivalent, and the reason this returns a
 * plain Boolean instead of pretending a chooser appeared.
 */
expect suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean

/**
 * Checks if the Snapchat application is installed and available to receive shares.
 */
expect fun isSnapchatAvailable(): Boolean

/**
 * Shares a 9:16 music card to Snapchat using Snapchat Creative Kit.
 *
 * Passes the pre-rendered music card as media, track caption, and a deep-link attachment
 * that powers the swipe-up Context Card in Snapchat.
 */
expect suspend fun shareToSnapchat(
    bytes: ByteArray,
    fileName: String,
    songTitle: String,
    artistName: String,
    deepLinkUrl: String,
    caption: String = "🎵 Listen on Pulse Music",
): Boolean
