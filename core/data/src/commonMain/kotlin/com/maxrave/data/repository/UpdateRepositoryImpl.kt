package com.maxrave.data.repository

import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
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

            // Always add high-availability primary and fallback endpoints
            endpoints.add("https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest")
            endpoints.add("https://muthumanikandanb2005-ux.github.io/fdroid-repo/version.json")
            endpoints.add("https://raw.githubusercontent.com/muthumanikandanb2005-ux/PulseMusic/main/version.json")

            var lastError = "Failed to connect to update servers"
            for (endpoint in endpoints.distinct()) {
                val res = youTube.checkForGithubReleaseUpdate(endpoint)
                if (res.isSuccess) {
                    val response = res.getOrNull()
                    if (response != null && !response.tagName.isNullOrBlank()) {
                        val updateAssets = response.assets?.mapNotNull { asset ->
                            val url = asset?.browserDownloadUrl ?: return@mapNotNull null
                            val name = asset.name ?: ""
                            val size = asset.size?.toLong() ?: 0L
                            com.maxrave.domain.data.model.update.UpdateAsset(
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
                    lastError = res.exceptionOrNull()?.localizedMessage ?: lastError
                }
            }
            emit(Resource.Error<UpdateData>(lastError))
        }.flowOn(Dispatchers.IO)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            emit(Resource.Error<UpdateData>("F-Droid updates disabled"))
        }.flowOn(Dispatchers.IO)
}