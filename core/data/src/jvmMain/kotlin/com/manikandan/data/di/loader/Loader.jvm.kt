package com.manikandan.data.di.loader

import com.pulsemusic.media_jvm.di.loadDesktopPlayerModule

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}
