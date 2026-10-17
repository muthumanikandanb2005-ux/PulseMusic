package com.manikandan.domain.data.model.update

data class UpdateAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long = 0L,
)

data class UpdateData(
    val tagName: String,
    val releaseTime: String?,
    val body: String,
    val assets: List<UpdateAsset> = emptyList(),
) {
    fun getWindowsSetupAsset(): UpdateAsset? =
        assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }

    fun getAndroidApkAsset(): UpdateAsset? =
        assets.firstOrNull { it.name.contains("universal", ignoreCase = true) && it.name.endsWith(".apk", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}