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
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException

class WindowsSafeFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
    override fun atomicMove(source: Path, target: Path) {
        val isWindows = System.getProperty("os.name", "").contains("Windows", ignoreCase = true)
        if (!isWindows) {
            super.atomicMove(source, target)
            return
        }

        var lastError: Exception? = null
        // 1. Try standard atomicMove with retries in case of transient lock by antivirus or indexer
        for (attempt in 1..5) {
            try {
                super.atomicMove(source, target)
                return
            } catch (e: Exception) {
                lastError = e
                try {
                    Thread.sleep(20L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        // 2. Fallback for Windows NTFS where atomicMove (MoveFileEx with MOVEFILE_REPLACE_EXISTING)
        // throws java.nio.file.AccessDeniedException when target exists or is locked.
        val sourceFile = source.toFile()
        val targetFile = target.toFile()

        if (!sourceFile.exists()) {
            throw lastError ?: IOException("Source file $source does not exist during atomicMove")
        }

        // Try Java NIO non-atomic replace (Files.copy with REPLACE_EXISTING) + delete source
        for (attempt in 1..5) {
            try {
                if (targetFile.exists()) {
                    targetFile.setWritable(true)
                }
                java.nio.file.Files.copy(
                    sourceFile.toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                sourceFile.delete()
                return
            } catch (e: Exception) {
                lastError = e
                try {
                    Thread.sleep(25L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        // 3. Fallback: Direct stream copy + delete source
        for (attempt in 1..5) {
            try {
                if (targetFile.exists()) {
                    targetFile.setWritable(true)
                }
                sourceFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                sourceFile.delete()
                return
            } catch (e: Exception) {
                lastError = e
                try {
                    Thread.sleep(30L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        throw lastError ?: IOException("Failed to move $source to $target on Windows after all strategies")
    }
}

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
            fileSystem = WindowsSafeFileSystem(FileSystem.SYSTEM),
            serializer = PreferencesSerializer,
            producePath = {
                val file = File(dir, "$SETTINGS_FILENAME.preferences_pb")
                file.absolutePath.toPath()
            }
        )
    )
}

actual fun createDataStoreInstance(): DataStore<Preferences> = dataStoreInstance