package com.maxrave.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import createDataStore
import java.io.File

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val dir = File(getHomeFolderPath(listOf(".simpmusic")))
    if (!dir.exists()) {
        dir.mkdirs()
    }
    // Clean up any stale temp file that Windows may have left locked or orphaned from a crash
    val tmpFile = File(dir, "$SETTINGS_FILENAME.preferences_pb.tmp")
    if (tmpFile.exists()) {
        try {
            tmpFile.delete()
        } catch (_: Exception) {}
    }
    createDataStore(
        producePath = {
            val file = File(dir, "$SETTINGS_FILENAME.preferences_pb")
            file.absolutePath
        }
    )
}

actual fun createDataStoreInstance(): DataStore<Preferences> = dataStoreInstance