package com.pulse.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.music.ui.icon.Close
import com.pulse.music.ui.icon.PlaylistAdd
import com.pulse.music.ui.icon.SimpIcons
import com.pulse.music.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.import_data
import simpmusic.composeapp.generated.resources.import_link_dialog_subtitle
import simpmusic.composeapp.generated.resources.import_link_dialog_title
import simpmusic.composeapp.generated.resources.import_link_placeholder

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportLinkDialog(
    onDismiss: () -> Unit,
    onImportUrl: (url: String) -> Unit,
) {
    var urlText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        icon = {
            Icon(
                imageVector = SimpIcons.PlaylistAdd,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "Import Playlist from Other Streaming Apps",
                style = typo().titleMedium,
                color = Color.White,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Transfer playlists directly from Spotify, Apple Music, YouTube Music, Deezer, and other streaming services into Pulse Music.",
                    style = typo().bodyMedium,
                    color = Color(0xFF8E8E93),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Supported platform chips in grey and white AMOLED styling
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Spotify", "Apple Music", "YouTube Music", "Deezer", "JioSaavn", "Amazon Music").forEach { service ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222226),
                        ) {
                            Text(
                                text = service,
                                style = typo().labelSmall.copy(fontSize = 11.sp),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = {
                        Text(
                            text = "Paste playlist URL (e.g. spotify.com/playlist/...)",
                            style = typo().bodySmall,
                            color = Color(0xFF8E8E93),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF2C2C2E),
                        cursorColor = Color.White,
                        focusedContainerColor = Color(0xFF222226),
                        unfocusedContainerColor = Color(0xFF222226),
                    ),
                    trailingIcon = {
                        if (urlText.isNotBlank()) {
                            IconButton(onClick = { urlText = "" }) {
                                Icon(
                                    imageVector = SimpIcons.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF8E8E93),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = urlText.trim()
                    if (trimmed.isNotBlank()) {
                        onImportUrl(trimmed)
                        onDismiss()
                    }
                },
                enabled = urlText.isNotBlank(),
            ) {
                Text(text = "Import", color = if (urlText.isNotBlank()) Color.White else Color(0xFF8E8E93))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color(0xFF8E8E93))
            }
        },
    )
}
