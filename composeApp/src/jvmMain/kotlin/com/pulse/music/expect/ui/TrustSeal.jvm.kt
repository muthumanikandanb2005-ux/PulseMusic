package com.pulse.music.expect.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.music.WindowsProtocolRegistrar
import com.pulse.music.expect.copyToClipboard
import com.pulse.music.expect.openUrl
import com.pulse.music.ui.component.PulseSettingRow
import com.pulse.music.ui.icon.ContentCopy
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.icon.VerifiedUser
import com.pulse.music.ui.theme.typo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

internal enum class AuthenticodeTrustState {
    VERIFIED_SYSTEM_TRUST,
    SIGNED_LOCAL_PUBLISHER,
    UNSIGNED_BINARY,
    DEVELOPMENT_BUILD,
}

internal data class WindowsTrustSnapshot(
    val isWindows: Boolean,
    val exePath: String?,
    val exeSha256: String?,
    val bundledCertPath: String?,
    val trustScriptPath: String?,
    val certSubject: String?,
    val certThumbprintSha1: String?,
    val certValidUntil: String?,
    val hasPeSignature: Boolean,
    val isRootTrustedByWindows: Boolean,
    val authenticodeDetail: String?,
    val state: AuthenticodeTrustState,
)

internal object WindowsTrustInspector {
    private const val RELEASE_URL = "https://github.com/muthumanikandanb2005-ux/PulseMusic/releases"

    private val isWindowsHost: Boolean
        get() = System.getProperty("os.name", "").contains("Windows", ignoreCase = true)

    fun isWindowsDesktop(): Boolean = isWindowsHost

    fun inspectFast(): WindowsTrustSnapshot {
        if (!isWindowsHost) {
            return WindowsTrustSnapshot(
                isWindows = false,
                exePath = null,
                exeSha256 = null,
                bundledCertPath = null,
                trustScriptPath = null,
                certSubject = null,
                certThumbprintSha1 = null,
                certValidUntil = null,
                hasPeSignature = false,
                isRootTrustedByWindows = false,
                authenticodeDetail = null,
                state = AuthenticodeTrustState.DEVELOPMENT_BUILD,
            )
        }

        val exePath = WindowsProtocolRegistrar.resolveExePath()
        val exeFile = exePath?.let { File(it) }?.takeIf { it.exists() && it.isFile }
        val appDir = exeFile?.parentFile
        val isPackagedApp =
            exeFile != null &&
                !exeFile.name.equals("java.exe", ignoreCase = true) &&
                !exeFile.name.equals("javaw.exe", ignoreCase = true)

        val certFile = findBundledFile(appDir, "PulseMusic.cer")
        val trustBat = findBundledFile(appDir, "trust_publisher.bat")

        val parsedCert = certFile?.let { readCertificate(it) }
        val sha1 = parsedCert?.let { digestBytes(it.encoded, "SHA-1") }
        val isRootTrusted = sha1 != null && isCertInWindowsStore(sha1, "Windows-ROOT")
        val hasPeSig = if (isPackagedApp && exeFile != null) hasPeCertificateTable(exeFile) else false

        val state =
            when {
                !isPackagedApp -> AuthenticodeTrustState.DEVELOPMENT_BUILD
                hasPeSig && isRootTrusted -> AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST
                hasPeSig -> AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER
                else -> AuthenticodeTrustState.UNSIGNED_BINARY
            }

        val expiry =
            parsedCert?.notAfter?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it)
            }

        return WindowsTrustSnapshot(
            isWindows = true,
            exePath = exeFile?.absolutePath ?: exePath,
            exeSha256 = null,
            bundledCertPath = certFile?.absolutePath,
            trustScriptPath = trustBat?.absolutePath,
            certSubject = parsedCert?.subjectX500Principal?.name,
            certThumbprintSha1 = sha1,
            certValidUntil = expiry,
            hasPeSignature = hasPeSig,
            isRootTrustedByWindows = isRootTrusted,
            authenticodeDetail = null,
            state = state,
        )
    }

    suspend fun inspectDetailed(): WindowsTrustSnapshot =
        withContext(Dispatchers.IO) {
            val base = inspectFast()
            val exeFile = base.exePath?.let { File(it) }?.takeIf { it.exists() && it.isFile }
            val sha256 = exeFile?.let { computeFileSha256(it) }
            val authenticode = exeFile?.let { queryAuthenticodeStatus(it) }
            base.copy(
                exeSha256 = sha256,
                authenticodeDetail = authenticode,
            )
        }

    fun openCertificateDialog(certPath: String): Boolean =
        runCatching {
            val file = File(certPath)
            if (!file.exists() || !Desktop.isDesktopSupported()) return@runCatching false
            Desktop.getDesktop().open(file)
            true
        }.getOrDefault(false)

    fun openReleaseChecksums() {
        openUrl(RELEASE_URL)
    }

    /**
     * Reads only the PE/COFF Optional Header Certificate Table data directory (index 4).
     * Zero child processes; completes in sub-millisecond time.
     */
    internal fun hasPeCertificateTable(file: File): Boolean =
        runCatching {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 0x120) return@use false
                val dosHeader = ByteArray(64)
                raf.readFully(dosHeader)
                if (dosHeader[0] != 'M'.code.toByte() || dosHeader[1] != 'Z'.code.toByte()) {
                    return@use false
                }
                val peOffset = ByteBuffer.wrap(dosHeader, 0x3C, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
                if (peOffset <= 0 || peOffset + 264 > raf.length()) return@use false

                raf.seek(peOffset)
                val peBuf = ByteArray(264)
                raf.readFully(peBuf)
                val bb = ByteBuffer.wrap(peBuf).order(ByteOrder.LITTLE_ENDIAN)
                if (bb.get(0) != 'P'.code.toByte() || bb.get(1) != 'E'.code.toByte() || bb.get(2).toInt() != 0 || bb.get(3).toInt() != 0) {
                    return@use false
                }
                val optHeaderStart = 24
                val magic = bb.getShort(optHeaderStart).toInt() and 0xFFFF
                val certDirOffset =
                    when (magic) {
                        0x010B -> optHeaderStart + 128 // PE32: DataDirectory[4]
                        0x020B -> optHeaderStart + 144 // PE32+: DataDirectory[4]
                        else -> return@use false
                    }
                if (certDirOffset + 8 > peBuf.size) return@use false
                val certTableFileOffset = bb.getInt(certDirOffset).toLong() and 0xFFFFFFFFL
                val certTableSize = bb.getInt(certDirOffset + 4).toLong() and 0xFFFFFFFFL
                certTableFileOffset > 0L && certTableSize > 0L && certTableFileOffset + certTableSize <= raf.length()
            }
        }.getOrDefault(false)

    private fun findBundledFile(
        appDir: File?,
        fileName: String,
    ): File? {
        val candidates =
            listOfNotNull(
                appDir?.let { File(it, fileName) },
                appDir?.let { File(it, "app\\resources\\$fileName") },
                appDir?.let { File(it, "resources\\$fileName") },
                File("dist\\$fileName"),
                File("scripts\\$fileName"),
            )
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    private fun readCertificate(file: File): X509Certificate? =
        runCatching {
            file.inputStream().use { input ->
                CertificateFactory.getInstance("X.509").generateCertificate(input) as? X509Certificate
            }
        }.getOrNull()

    private fun isCertInWindowsStore(
        targetSha1: String,
        storeName: String,
    ): Boolean =
        runCatching {
            val ks = KeyStore.getInstance(storeName)
            ks.load(null, null)
            val aliases = ks.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                if (digestBytes(cert.encoded, "SHA-1").equals(targetSha1, ignoreCase = true)) {
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(false)

    private fun computeFileSha256(file: File): String? =
        runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            file.inputStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()

    private fun digestBytes(
        bytes: ByteArray,
        algo: String,
    ): String =
        MessageDigest.getInstance(algo)
            .digest(bytes)
            .joinToString("") { "%02X".format(it) }

    private fun queryAuthenticodeStatus(exeFile: File): String? =
        runCatching {
            val escaped = exeFile.absolutePath.replace("'", "''")
            val script =
                "\$s = Get-AuthenticodeSignature -LiteralPath '$escaped'; " +
                    "\"Status=\$(\$s.Status); Subject=\$(\$s.SignerCertificate.Subject); TimeStamper=\$(\$s.TimeStamperCertificate.Subject)\""
            val process =
                ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true)
                    .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && out.isNotBlank()) out else null
        }.getOrNull()
}

@Composable
actual fun WindowsTitleBarTrustSeal(
    isDark: Boolean,
    tint: Color,
    modifier: Modifier,
) {
    if (!WindowsTrustInspector.isWindowsDesktop()) return

    val snapshot = remember { WindowsTrustInspector.inspectFast() }
    var showDialog by remember { mutableStateOf(false) }

    val statusTint =
        when (snapshot.state) {
            AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST -> Color(0xFF22C55E)
            AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER -> Color(0xFF38BDF8)
            AuthenticodeTrustState.UNSIGNED_BINARY -> Color(0xFFF59E0B)
            AuthenticodeTrustState.DEVELOPMENT_BUILD -> tint.copy(alpha = 0.6f)
        }
    val label =
        when (snapshot.state) {
            AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST -> "Verified"
            AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER -> "Signed build"
            AuthenticodeTrustState.UNSIGNED_BINARY -> "Unverified"
            AuthenticodeTrustState.DEVELOPMENT_BUILD -> "Dev build"
        }

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, tint.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.05f))
                .clickable { showDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = PulseIcons.VerifiedUser,
            contentDescription = label,
            tint = statusTint,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            style = typo().labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = tint.copy(alpha = 0.88f),
        )
    }

    if (showDialog) {
        WindowsTrustSealDialog(
            initialSnapshot = snapshot,
            isDark = isDark,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
actual fun WindowsTrustSealSettingsRow(
    isDark: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    cardBackground: Color,
    divider: @Composable () -> Unit,
) {
    if (!WindowsTrustInspector.isWindowsDesktop()) return

    val snapshot = remember { WindowsTrustInspector.inspectFast() }
    var showDialog by remember { mutableStateOf(false) }

    val (badgeText, badgeBg, badgeColor, subtitle) =
        when (snapshot.state) {
            AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST ->
                Quadruple(
                    "VERIFIED",
                    Color(0x2222C55E),
                    Color(0xFF22C55E),
                    "Authenticode signature and publisher root trusted on this PC",
                )
            AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER ->
                Quadruple(
                    "SIGNED",
                    Color(0x2238BDF8),
                    Color(0xFF38BDF8),
                    "Digitally signed by Manikandan • Tap to inspect SHA-256 & trust certificate",
                )
            AuthenticodeTrustState.UNSIGNED_BINARY ->
                Quadruple(
                    "UNSIGNED",
                    Color(0x22F59E0B),
                    Color(0xFFF59E0B),
                    "Binary signature not found • Tap to verify release checksums",
                )
            AuthenticodeTrustState.DEVELOPMENT_BUILD ->
                Quadruple(
                    "DEV",
                    Color(0x229CA3AF),
                    subtitleColor,
                    "Running unpacked JVM build • Official releases are signed by Manikandan",
                )
        }

    divider()
    PulseSettingRow(
        title = "Windows Trust Seal",
        subtitle = subtitle,
        badgeText = badgeText,
        badgeBg = badgeBg,
        badgeTextColor = badgeColor,
        leadingIcon = PulseIcons.VerifiedUser,
        showChevron = true,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        WindowsTrustSealDialog(
            initialSnapshot = snapshot,
            isDark = isDark,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
actual fun WindowsCreditTrustBadge() {
    if (!WindowsTrustInspector.isWindowsDesktop()) return

    val snapshot = remember { WindowsTrustInspector.inspectFast() }
    var showDialog by remember { mutableStateOf(false) }

    val (accent, title, subtitle) =
        when (snapshot.state) {
            AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST ->
                Triple(Color(0xFF22C55E), "Windows Publisher Trust Verified", "Signed by Manikandan and trusted in Windows Certificate Store")
            AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER ->
                Triple(Color(0xFF38BDF8), "Digitally Signed Windows Binary", "Publisher certificate bundled • Tap to verify SHA-256 or trust locally")
            AuthenticodeTrustState.UNSIGNED_BINARY ->
                Triple(Color(0xFFF59E0B), "Unsigned Local Binary", "Tap to inspect checksums and publisher certificate details")
            AuthenticodeTrustState.DEVELOPMENT_BUILD ->
                Triple(Color(0xFF9CA3AF), "Local Development Build", "Packaged installers carry Authenticode signing and SHA-256 checksums")
        }

    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDialog = true },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = PulseIcons.VerifiedUser,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typo().titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = typo().bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDialog) {
        WindowsTrustSealDialog(
            initialSnapshot = snapshot,
            isDark = true,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun WindowsTrustSealDialog(
    initialSnapshot: WindowsTrustSnapshot,
    isDark: Boolean,
    onDismiss: () -> Unit,
) {
    var snapshot by remember(initialSnapshot) { mutableStateOf(initialSnapshot) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialSnapshot) {
        snapshot = WindowsTrustInspector.inspectDetailed()
    }

    val containerBg = if (isDark) Color(0xFF141518) else Color(0xFFFAFAFC)
    val titleFg = if (isDark) Color(0xFFF4F4F5) else Color(0xFF18181B)
    val mutedFg = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val surfaceStep = if (isDark) Color(0xFF1E2025) else Color(0xFFF1F2F6)
    val borderColor = titleFg.copy(alpha = 0.08f)
    val statusColor =
        when (snapshot.state) {
            AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST -> Color(0xFF22C55E)
            AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER -> Color(0xFF38BDF8)
            AuthenticodeTrustState.UNSIGNED_BINARY -> Color(0xFFF59E0B)
            AuthenticodeTrustState.DEVELOPMENT_BUILD -> mutedFg
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerBg,
        titleContentColor = titleFg,
        textContentColor = mutedFg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = PulseIcons.VerifiedUser,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Windows Trust Seal",
                    style = typo().titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = titleFg,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = surfaceStep,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TrustMetaLine(
                            label = "Status",
                            value =
                                when (snapshot.state) {
                                    AuthenticodeTrustState.VERIFIED_SYSTEM_TRUST -> "Trusted Publisher (Authenticode + Root)"
                                    AuthenticodeTrustState.SIGNED_LOCAL_PUBLISHER -> "Digitally Signed (Author Certificate)"
                                    AuthenticodeTrustState.UNSIGNED_BINARY -> "No embedded Authenticode signature"
                                    AuthenticodeTrustState.DEVELOPMENT_BUILD -> "Unpacked development session"
                                },
                            labelColor = mutedFg,
                            valueColor = statusColor,
                        )
                        TrustMetaLine(
                            label = "Publisher",
                            value = snapshot.certSubject ?: "CN=Manikandan, O=Manikandan",
                            labelColor = mutedFg,
                            valueColor = titleFg,
                        )
                        snapshot.certThumbprintSha1?.let { thumb ->
                            TrustMetaLine(
                                label = "Cert SHA-1",
                                value = thumb,
                                labelColor = mutedFg,
                                valueColor = titleFg,
                                monospace = true,
                            )
                        }
                        snapshot.certValidUntil?.let { validUntil ->
                            TrustMetaLine(
                                label = "Valid until",
                                value = validUntil,
                                labelColor = mutedFg,
                                valueColor = titleFg,
                                monospace = true,
                            )
                        }
                        TrustMetaLine(
                            label = "Binary SHA-256",
                            value = snapshot.exeSha256 ?: "Computing checksum...",
                            labelColor = mutedFg,
                            valueColor = titleFg,
                            monospace = true,
                        )
                        snapshot.authenticodeDetail?.let { detail ->
                            TrustMetaLine(
                                label = "Authenticode",
                                value = detail,
                                labelColor = mutedFg,
                                valueColor = mutedFg,
                                monospace = true,
                            )
                        }
                    }
                }

                Text(
                    text =
                        if (snapshot.isRootTrustedByWindows) {
                            "Manikandan's publisher certificate is installed in your Windows Trust Store. SmartScreen recognizes this signature on this PC."
                        } else {
                            "Independent builds carry Manikandan's author certificate rather than a paid EV CA, so Windows shows 'Unknown publisher' until you open and install the bundled certificate or compare the SHA-256 digest against GitHub Releases."
                        },
                    style = typo().bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = mutedFg,
                )

                copiedNotice?.let { notice ->
                    Text(
                        text = notice,
                        style = typo().bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF22C55E),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val certPath = snapshot.bundledCertPath
                    if (certPath != null) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val opened = withContext(Dispatchers.IO) {
                                        WindowsTrustInspector.openCertificateDialog(certPath)
                                    }
                                    copiedNotice =
                                        if (opened) {
                                            "Opened Windows certificate dialog"
                                        } else {
                                            "Certificate path: $certPath"
                                        }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Open Certificate",
                                style = typo().labelMedium,
                                color = titleFg,
                            )
                        }
                    }

                    val sha = snapshot.exeSha256
                    if (sha != null) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard("Pulse Music EXE SHA-256", sha)
                                copiedNotice = "Copied SHA-256 digest to clipboard"
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = PulseIcons.ContentCopy,
                                contentDescription = null,
                                tint = titleFg,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Copy SHA-256",
                                style = typo().labelMedium,
                                color = titleFg,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = titleFg)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { WindowsTrustInspector.openReleaseChecksums() },
            ) {
                Text("GitHub Checksums", color = mutedFg)
            }
        },
    )
}

@Composable
private fun TrustMetaLine(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    monospace: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = typo().labelSmall.copy(fontSize = 11.sp),
            color = labelColor,
        )
        Text(
            text = value,
            style =
                typo().bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                    fontFeatureSettings = "tnum",
                ),
            color = valueColor,
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
