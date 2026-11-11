package com.manikandan.domain.repository

import com.manikandan.domain.data.model.update.UpdateData
import com.manikandan.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>>
    fun checkForFdroidUpdate(): Flow<Resource<UpdateData>>
}