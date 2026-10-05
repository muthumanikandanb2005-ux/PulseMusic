package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.help_build_lyrics_database
import simpmusic.composeapp.generated.resources.help_build_lyrics_database_description
import simpmusic.composeapp.generated.resources.later
import simpmusic.composeapp.generated.resources.ok

@Composable
@ExperimentalMaterial3Api
fun ShareSavedLyricsDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (
        contributor: Pair<String, String>?,
    ) -> Unit, // contributor name and email, null if anonymous
) {
    AlertDialog(
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest.invoke()
                onConfirm(null)
            }) {
                Text(
                    stringResource(Res.string.ok),
                    style = typo().bodySmall,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest.invoke()
            }) {
                Text(
                    stringResource(Res.string.later),
                    style = typo().bodySmall,
                )
            }
        },
        title = {
            Text(
                stringResource(Res.string.help_build_lyrics_database),
                style = typo().labelSmall,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(Res.string.help_build_lyrics_database_description),
                    style = typo().bodySmall,
                )
            }
        },
    )
}