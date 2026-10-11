package com.maxrave.simpmusic.expect

import com.maxrave.domain.data.model.update.UpdateData

expect suspend fun downloadAndInstallAppUpdate(
    update: UpdateData,
    onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit,
    onReadyToInstall: () -> Unit,
    onError: (String) -> Unit,
)
