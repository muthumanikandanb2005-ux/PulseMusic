package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.import_data
import simpmusic.composeapp.generated.resources.import_failed
import simpmusic.composeapp.generated.resources.import_progress_songs
import simpmusic.composeapp.generated.resources.import_reading_file
import simpmusic.composeapp.generated.resources.import_result
import simpmusic.composeapp.generated.resources.import_result_skipped
import simpmusic.composeapp.generated.resources.ok

@Composable
fun ImportProgressDialog(
    progress: ImportProgress,
    onDismiss: () -> Unit,
) {
    val finished = progress is ImportProgress.Success || progress is ImportProgress.Error
    AlertDialog(
        onDismissRequest = { if (finished) onDismiss() },
        properties =
            DialogProperties(
                dismissOnBackPress = finished,
                dismissOnClickOutside = finished,
            ),
        title = {
            Text(
                text =
                    stringResource(
                        if (progress is ImportProgress.Error) Res.string.import_failed else Res.string.import_data,
                    ),
                style = typo().titleSmall,
            )
        },
        text = {
            Column {
                when (progress) {
                    is ImportProgress.Preparing -> {
                        Text(
                            text = stringResource(Res.string.import_reading_file),
                            style = typo().bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    is ImportProgress.Importing -> {
                        Text(
                            text = stringResource(Res.string.import_progress_songs, progress.processed, progress.total),
                            style = typo().bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (progress.total > 0) progress.processed.toFloat() / progress.total else 0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is ImportProgress.Success -> {
                        Text(
                            text =
                                stringResource(
                                    Res.string.import_result,
                                    progress.result.playlistsCreated,
                                    progress.result.songsImported,
                                ),
                            style = typo().bodyMedium,
                        )
                        if (progress.result.skippedEntries > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.import_result_skipped, progress.result.skippedEntries),
                                style = typo().bodySmall,
                            )
                        }
                    }

                    is ImportProgress.Error -> {
                        Text(
                            text = progress.message,
                            style = typo().bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.ok))
                }
            }
        },
    )
}
