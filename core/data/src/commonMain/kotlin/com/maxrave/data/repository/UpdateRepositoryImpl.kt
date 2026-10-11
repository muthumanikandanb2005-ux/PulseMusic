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
            val endpoint = when {
                rawEndpoint.isBlank() -> "https://api.github.com/repos/muthumanikandanb2005-ux/PulseMusic/releases/latest"
                !rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://") && rawEndpoint.contains("/") ->
                    "https://api.github.com/repos/$rawEndpoint/releases/latest"
                else -> rawEndpoint
            }
            youTube
                .checkForGithubReleaseUpdate(endpoint)
                .onSuccess { response ->
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
                }.onFailure {
                    emit(Resource.Error<UpdateData>(it.localizedMessage ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            emit(Resource.Error<UpdateData>("F-Droid updates disabled"))
        }.flowOn(Dispatchers.IO)
}