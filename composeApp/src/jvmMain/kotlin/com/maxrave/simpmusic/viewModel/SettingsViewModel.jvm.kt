package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.common.DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import com.manikandan.domain.repository.CacheRepository
import com.manikandan.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.zipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.restore_success
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

private fun getPreferencesFile(): File {
    val simpFile = File(getHomeFolderPath(listOf(".simpmusic")), "$SETTINGS_FILENAME.preferences_pb")
    val pulseFile = File(getHomeFolderPath(listOf(".pulsemusic")), "$SETTINGS_FILENAME.preferences_pb")
    if (pulseFile.exists() && !simpFile.exists()) {
        try {
            simpFile.parentFile?.mkdirs()
            pulseFile.copyTo(simpFile, overwrite = true)
        } catch (_: Exception) {}
    }
    return simpFile
}

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        ZipInputStream(
            FileInputStream(File(uri.toString())),
        ).use { inputStream ->
            var entry =
                try {
                    inputStream.nextEntry
                } catch (e: Exception) {
                    null
                }
            while (entry != null) {
                Logger.d("BackupRestore", "Processing entry: ${entry.name}")
                when {
                    entry.name == "$SETTINGS_FILENAME.preferences_pb" -> {
                        val prefFile = getPreferencesFile()
                        prefFile.parentFile?.mkdirs()
                        prefFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    entry.name == DB_NAME -> {
                        commonRepository.databaseDaoCheckpoint()
                        commonRepository.closeDatabase()
                        FileOutputStream(commonRepository.getDatabasePath()).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                entry = inputStream.nextEntry
            }
        }
    }
    withContext(Dispatchers.Main) {
        showToast(getString(Res.string.restore_success), ToastGravity.Bottom)
        showToast("App will restart to apply changes", ToastGravity.Bottom)
        delay(2000)
        exitProcess(0)
    }
}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    withContext(Dispatchers.IO) {
        FileOutputStream(File(uri.toString())).use { fos ->
            fos.buffered().zipOutputStream().use { outputStream ->
                val prefFile = getPreferencesFile()
                if (prefFile.exists()) {
                    prefFile
                        .inputStream()
                        .buffered()
                        .use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("$SETTINGS_FILENAME.preferences_pb"))
                            inputStream.copyTo(outputStream)
                        }
                }
                commonRepository.databaseDaoCheckpoint()
                FileInputStream(commonRepository.getDatabasePath()).use { inputStream ->
                    outputStream.putNextEntry(ZipEntry(DB_NAME))
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

actual fun getPackageName(): String = ""

actual fun getFileDir(): String = ""

actual fun changeLanguageNative(code: String) {
    Locale.setDefault(
        Locale.forLanguageTag(
            if (code == "id-ID") {
                "in-ID"
            } else {
                code
            },
        ),
    )
}