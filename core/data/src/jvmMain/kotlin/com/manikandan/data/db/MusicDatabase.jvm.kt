package com.manikandan.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import com.manikandan.common.DB_NAME
import com.manikandan.data.io.getHomeFolderPath
import java.io.File

actual fun getDatabaseBuilder(
    converters: Converters
): RoomDatabase.Builder<MusicDatabase> {
    return Room.databaseBuilder<MusicDatabase>(
        name = getDatabasePath()
    ).addTypeConverter(converters)
}

actual fun getDatabasePath(): String {
    val dbFile = File(getHomeFolderPath(listOf(".pulsemusic", "db")), DB_NAME)
    return dbFile.absolutePath
}