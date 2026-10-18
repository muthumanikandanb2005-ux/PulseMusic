package com.maxrave.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import createDataStore
import java.io.File

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path.Companion.toPath

private val dataStoreInstance: DataStore<Preferences> by lazy {
    val dir = File(getHomeFolderPath(listOf(".simpmusic")))
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val tmpFile = File(dir, "$SETTINGS_FILENAME.preferences_pb.tmp")
    if (tmpFile.exists()) {
        try {
            tmpFile.delete()
        } catch (_: Exception) {}
    }
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = {
                val file = File(dir, "$SETTINGS_FILENAME.preferences_pb")
                file.absolutePath.toPath()
            }
        )
    )
}

actual fun createDataStoreInstance(): DataStore<Preferences> = dataStoreInstance