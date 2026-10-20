package com.maxrave.data.repository

import com.manikandan.domain.data.model.update.UpdateData
import com.manikandan.domain.manager.DataStoreManager
import com.manikandan.domain.repository.UpdateRepository
import com.manikandan.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class UpdateRepositoryImpl(
    private val youTube: YouTube,
    private val dataStoreManager: DataStoreManager,
) : UpdateRepository {
    override fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>> =
        flow {
            val rawEndpoint = dataStoreManager.customUpdateEndpoint.first().trim()
            val endpoints = mutableListOf<String>()

            // 1. Primary fast CDN endpoint (never rate limits)
            endpoints.add("https://cdn.jsdelivr.net/gh/muthumanikandanb2005-ux/PulseMusic@main/version.json")
            // 2. Secondary raw GitHub endpoint
            endpoints.add("https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json")

            // 3. Custom endpoint if configured
            if (rawEndpoint.isNotBlank() &&
                !rawEndpoint.contains("maxrave-dev", ignoreCase = true) &&
                !rawEndpoint.contains("simpmusic", ignoreCase = true)
            ) {
                val formatted = if (!rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://") && rawEndpoint.contains("/")) {
                    "https://api.github.com/repos/$rawEndpoint/releases/latest"
                } else {
                    rawEndpoint
                }
                endpoints.add(formatted)
            }

            // 4. Fallback GitHub API endpoint
            endpoints.add("https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest")

            var lastError = "Unable to check for updates. Please try again later."
            for (endpoint in endpoints.distinct()) {
                val res = youTube.checkForGithubReleaseUpdate(endpoint)
                if (res.isSuccess) {
                    val response = res.getOrNull()
                    if (response != null && !response.tagName.isNullOrBlank()) {
                        val updateAssets = response.assets?.mapNotNull { asset ->
                            val url = asset?.browserDownloadUrl ?: return@mapNotNull null
                            val name = asset.name ?: ""
                            val size = asset.size ?: 0L
                            com.manikandan.domain.data.model.update.UpdateAsset(
                                name = name,
                                downloadUrl = url,
                                size = size,
                            )
                        } ?: emptyList()

                        emit(
                            Resource.Success(
                                UpdateData(
                                    tagName = response.tagName ?: "",
                                    releaseTime = response.publishedAt ?: "",
                                    body = response.body ?: "",
                                    assets = updateAssets,
                                ),
                            ),
                        )
                        return@flow
                    }
                } else {
                    Logger.w("UpdateRepository", "Endpoint failed: $endpoint - ${res.exceptionOrNull()?.message}")
                }
            }
            emit(Resource.Error<UpdateData>("Unable to check for updates. Please try again later."))
        }.flowOn(Dispatchers.IO)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            emit(Resource.Error<UpdateData>("F-Droid updates disabled"))
        }.flowOn(Dispatchers.IO)
}