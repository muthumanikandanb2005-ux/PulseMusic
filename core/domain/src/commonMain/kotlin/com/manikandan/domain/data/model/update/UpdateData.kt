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
        assets.firstOrNull {
            it.name.endsWith(".exe", ignoreCase = true) ||
            it.name.endsWith(".msi", ignoreCase = true) ||
            (it.name.contains("windows", ignoreCase = true) && it.name.endsWith(".zip", ignoreCase = true))
        }

    fun getAndroidApkAsset(preferredAbi: String? = null): UpdateAsset? {
        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        if (!preferredAbi.isNullOrBlank()) {
            val matched = apkAssets.firstOrNull { it.name.contains(preferredAbi, ignoreCase = true) }
            if (matched != null) return matched
        }

        // Prioritize native 64-bit ARM architecture (33MB vs 61MB Universal)
        return apkAssets.firstOrNull { it.name.contains("arm64-v8a", ignoreCase = true) }
            ?: apkAssets.firstOrNull { it.name.contains("armeabi-v7a", ignoreCase = true) }
            ?: apkAssets.firstOrNull { it.name.contains("x86_64", ignoreCase = true) }
            ?: apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: apkAssets.firstOrNull()
    }
}
