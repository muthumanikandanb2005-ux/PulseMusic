package com.pulse.music.expect

import com.manikandan.domain.data.model.update.UpdateData

expect suspend fun downloadAndInstallAppUpdate(
    update: UpdateData,
    onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    onReadyToInstall: () -> Unit,
    onError: (String) -> Unit,
)
