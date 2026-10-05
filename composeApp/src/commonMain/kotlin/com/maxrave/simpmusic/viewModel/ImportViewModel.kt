package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.domain.repository.ImportRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.mohamedrejeb.calf.core.PlatformContext
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.import_invalid_file
import simpmusic.composeapp.generated.resources.import_link_invalid

import com.mohamedrejeb.calf.io.getPath

/**
 * Drives an import of a playlist file (Pulse JSON, generic JSON, M3U, M3U8, CSV, TSV, or TXT).
 */
class ImportViewModel(
    private val importRepository: ImportRepository,
) : BaseViewModel() {
    private val _importState: MutableStateFlow<ImportProgress?> = MutableStateFlow(null)

    /** `null` while idle; otherwise the latest step of the running or finished import. */
    val importState: StateFlow<ImportProgress?> = _importState.asStateFlow()

    private var importJob: Job? = null

    fun import(
        file: KmpFile,
        context: PlatformContext,
        defaultPlaylistTitle: String? = null,
    ) {
        importJob?.cancel()
        importJob =
            viewModelScope.launch {
                _importState.value = ImportProgress.Preparing
                val invalidFileMessage = getString(Res.string.import_invalid_file)
                val fallbackTitle = defaultPlaylistTitle
                    ?: runCatching {
                        file.getPath(context)?.substringAfterLast('/')?.substringAfterLast('\\')?.substringBeforeLast('.')
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Imported Playlist"

                val content =
                    withContext(Dispatchers.IO) {
                        runCatching { file.readByteArray(context).decodeToString() }
                    }.getOrElse { throwable ->
                        log("import: cannot read picked file - ${throwable.message}")
                        _importState.value = ImportProgress.Error(invalidFileMessage)
                        return@launch
                    }
                importRepository.import(content, invalidFileMessage, fallbackTitle).collect { progress ->
                    _importState.value = progress
                }
            }
    }

    fun importFromUrl(
        url: String,
        defaultPlaylistTitle: String? = null,
    ) {
        importJob?.cancel()
        importJob =
            viewModelScope.launch {
                _importState.value = ImportProgress.Preparing
                val invalidUrlMessage = runCatching { getString(Res.string.import_link_invalid) }.getOrNull()
                    ?: "Could not fetch or parse playlist from this link. Please check the URL and try again."
                importRepository.importFromUrl(url.trim(), invalidUrlMessage, defaultPlaylistTitle).collect { progress ->
                    _importState.value = progress
                }
            }
    }

    /** Back to idle, which is what dismisses the progress/result dialog. */
    fun dismiss() {
        importJob?.cancel()
        importJob = null
        _importState.value = null
    }
}
