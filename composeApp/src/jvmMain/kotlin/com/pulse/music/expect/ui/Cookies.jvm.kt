package com.pulse.music.expect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manikandan.common.Config
import com.pulse.music.expect.openUrl
import com.pulse.music.ui.icon.CheckCircle
import com.pulse.music.ui.icon.ContentCopy
import com.pulse.music.ui.icon.Info
import com.pulse.music.ui.icon.OpenInNew
import com.pulse.music.ui.icon.PlayArrow
import com.pulse.music.ui.icon.PulseIcons
import com.pulse.music.ui.theme.typo
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI

@Volatile
private var desktopCachedCookie: String = ""

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        override fun getCookie(url: String): String {
            if (desktopCachedCookie.isNotBlank()) {
                return desktopCachedCookie
            }
            return runCatching {
                CookieHandler
                    .getDefault()
                    ?.get(URI(url), emptyMap())?.get("Cookie")
                    ?.joinToString("; ") ?: ""
            }.getOrDefault("")
        }

        override fun removeAllCookies() {
            desktopCachedCookie = ""
            runCatching {
                CookieHandler.setDefault(CookieManager())
            }
        }
    }

/**
 * Sanitizes and extracts YouTube auth cookies from various formats:
 * - Raw cookie string: "SAPISID=...; __Secure-3PAPISID=...; LOGIN_INFO=..."
 * - Request header format: "cookie: SAPISID=...; ..."
 * - cURL commands: "curl '...' -H 'cookie: SAPISID=...; ...'"
 * - Netscape cookies.txt format
 */
fun sanitizeYouTubeCookie(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""

    // 1. If it's a cURL command, extract the cookie header
    val curlCookieRegex = Regex("""(?:-H|--header)\s+['"][Cc]ookie:\s*([^'"]+)['"]""")
    val curlMatch = curlCookieRegex.find(trimmed)
    if (curlMatch != null) {
        return curlMatch.groupValues[1].trim()
    }

    // 2. If it contains a "cookie:" header line in multi-line headers
    val lines = trimmed.lines()
    for (line in lines) {
        val lineTrimmed = line.trim()
        if (lineTrimmed.startsWith("cookie:", ignoreCase = true)) {
            val extracted = lineTrimmed.substringAfter(":").trim()
            if (extracted.isNotEmpty()) {
                return extracted
            }
        }
    }

    // 3. If it starts with "Cookie: " or "cookie: "
    if (trimmed.startsWith("cookie:", ignoreCase = true)) {
        return trimmed.substringAfter(":").trim()
    }

    // 4. Netscape format (tab-separated: domain, flag, path, secure, expiration, name, value)
    if (lines.size > 1 && lines.any { it.contains("\t") }) {
        val pairs = mutableListOf<String>()
        for (line in lines) {
            val lineClean = line.trim()
            if (lineClean.startsWith("#") || lineClean.isEmpty()) continue
            val parts = lineClean.split("\t")
            if (parts.size >= 7) {
                val name = parts[5].trim()
                val value = parts[6].trim()
                if (name.isNotEmpty()) {
                    pairs.add("$name=$value")
                }
            }
        }
        if (pairs.isNotEmpty()) {
            return pairs.joinToString("; ")
        }
    }

    // 5. Clean single or double quotes wrapped around the whole string
    var clean = trimmed
    if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
        clean = clean.substring(1, clean.length - 1).trim()
    }

    return clean
}

fun hasYouTubeAuthTokens(cookie: String): Boolean {
    val lower = cookie.lowercase()
    return lower.contains("sapisid=") ||
        lower.contains("__secure-3papisid=") ||
        lower.contains("login_info=") ||
        lower.contains("sid=")
}

@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var cookieInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val sanitizedCookie = remember(cookieInput) { sanitizeYouTubeCookie(cookieInput) }
    val isValid = remember(sanitizedCookie) { hasYouTubeAuthTokens(sanitizedCookie) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF0000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PulseIcons.PlayArrow,
                        contentDescription = "YouTube",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Sign in to YouTube Music",
                        style = typo().titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Connect your account to sync playlists, liked songs & subscriptions",
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Quick 3-Step Setup",
                        style = typo().titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionStep(
                        step = "1",
                        title = "Open YouTube Music in your browser",
                        description = "Make sure you are logged into your Google account on music.youtube.com.",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionStep(
                        step = "2",
                        title = "Copy your cookie or cURL command",
                        description = "In your browser, press F12 (Developer Tools) -> Network tab -> click any request to music.youtube.com -> right-click -> Copy -> Copy as cURL (or copy the 'cookie' request header).",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionStep(
                        step = "3",
                        title = "Paste & connect",
                        description = "Click 'Paste from Clipboard' below. Pulse will automatically parse and validate your credentials.",
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            openUrl("https://music.youtube.com")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2E2E),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = PulseIcons.OpenInNew,
                            contentDescription = "Open Browser",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open music.youtube.com in Browser",
                            style = typo().labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cookie Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Paste Cookie or cURL",
                            style = typo().titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    val cleaned = sanitizeYouTubeCookie(clipText)
                                    cookieInput = cleaned
                                    errorText = null
                                    showToast("Cookie pasted from clipboard", ToastGravity.Bottom)
                                } else {
                                    showToast("Clipboard is empty", ToastGravity.Bottom)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = PulseIcons.ContentCopy,
                                contentDescription = "Paste",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Paste from Clipboard", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = {
                            cookieInput = it
                            errorText = null
                        },
                        placeholder = {
                            Text(
                                text = "Paste cookie string, request header, or cURL command here...",
                                color = Color.Gray,
                                fontSize = 13.sp,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValid) Color(0xFFA788FA) else Color(0xFFFF0000),
                            unfocusedBorderColor = if (isValid) Color(0xFFA788FA).copy(alpha = 0.6f) else Color(0xFF444444),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Indicator
                    if (cookieInput.isNotBlank()) {
                        if (isValid) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = PulseIcons.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = Color(0xFFA788FA),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Valid YouTube authentication tokens detected",
                                    color = Color(0xFFA788FA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = PulseIcons.Info,
                                    contentDescription = "Hint",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Need SAPISID, __Secure-3PAPISID, or LOGIN_INFO from music.youtube.com",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    if (!errorText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorText!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val finalCookie = sanitizeYouTubeCookie(cookieInput)
                            if (finalCookie.isBlank()) {
                                errorText = "Please enter or paste your cookie first."
                                return@Button
                            }
                            if (!hasYouTubeAuthTokens(finalCookie)) {
                                errorText = "The pasted text does not contain required YouTube auth tokens (SAPISID, __Secure-3PAPISID, or LOGIN_INFO)."
                                return@Button
                            }

                            isConnecting = true
                            errorText = null
                            desktopCachedCookie = finalCookie
                            state.value = WebViewState.Finished

                            // Trigger page finished to execute addAccount in LogInScreen
                            onPageFinished(Config.YOUTUBE_MUSIC_MAIN_URL)
                        },
                        enabled = !isConnecting && cookieInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF333333),
                            disabledContentColor = Color.Gray,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Connecting to YouTube Music...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(text = "Connect YouTube Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        aboveContent()
    }
}

@Composable
private fun InstructionStep(
    step: String,
    title: String,
    description: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = step,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = typo().bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var tokenInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Discord Login",
                style = typo().titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your Discord authorization token to enable rich presence and scrobbling",
                style = typo().bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                placeholder = { Text("Paste Discord Token here...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text
                        if (!clip.isNullOrBlank()) {
                            tokenInput = clip.trim()
                        }
                    },
                ) {
                    Text("Paste Token")
                }
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            onLoginDone(tokenInput.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                ) {
                    Text("Connect Discord")
                }
            }
        }
        aboveContent()
    }
}
