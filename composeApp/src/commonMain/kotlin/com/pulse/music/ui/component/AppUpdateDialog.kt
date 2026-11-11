package com.pulse.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.music.viewModel.SharedViewModel

@Composable
fun AppUpdateDialog(viewModel: SharedViewModel) {
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateDownloadState by viewModel.updateDownloadState.collectAsStateWithLifecycle()

    val update = availableUpdate
    if (update != null && showUpdateDialog) {
        val isDownloading = updateDownloadState is SharedViewModel.UpdateDownloadState.Downloading
        val isReady = updateDownloadState is SharedViewModel.UpdateDownloadState.ReadyToInstall
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading && !isReady) {
                    viewModel.dismissUpdate()
                }
            },
            title = {
                Text(
                    text = "⚡ Pulse Music ${update.tagName} Available",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA788FA),
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new OTA update is ready with lag optimizations, smooth playback, and enhanced performance.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    when (val state = updateDownloadState) {
                        is SharedViewModel.UpdateDownloadState.Downloading -> {
                            Column {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFA788FA),
                                    trackColor = Color(0xFF333333),
                                )
                                val percent = (state.progress * 100).toInt()
                                val mbDown = ((state.downloadedBytes * 10L) / (1024L * 1024L)).toFloat() / 10f
                                val mbTot = ((state.totalBytes * 10L) / (1024L * 1024L)).toFloat() / 10f
                                Text(
                                    text = if (state.totalBytes > 0) {
                                        "Downloading: $percent% ($mbDown MB / $mbTot MB)"
                                    } else {
                                        "Downloading: $mbDown MB..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                )
                            }
                        }
                        is SharedViewModel.UpdateDownloadState.ReadyToInstall -> {
                            Text(
                                text = "Update downloaded! Installing and restarting Pulse Music...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA788FA),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        is SharedViewModel.UpdateDownloadState.Error -> {
                            Text(
                                text = "Error: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        is SharedViewModel.UpdateDownloadState.Idle -> {
                            Text(
                                text = "Tap 'Update Now' to automatically download and install inside the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloading && !isReady) {
                    Button(
                        onClick = {
                            viewModel.downloadAndInstallUpdate(update)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA788FA),
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = if (updateDownloadState is SharedViewModel.UpdateDownloadState.Error) "Retry Update" else "Update Now",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            dismissButton = {
                if (!isDownloading && !isReady) {
                    TextButton(onClick = {
                        viewModel.dismissUpdate()
                    }) {
                        Text("Later", color = Color(0xFFA788FA))
                    }
                }
            },
            containerColor = Color(0xFF1A1A1A),
        )
    }
}
