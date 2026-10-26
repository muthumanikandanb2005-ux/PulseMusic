package org.simpmusic.crashlytics

import android.content.Context
import android.util.Log
import com.manikandan.domain.data.player.PlayerError

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Log.e("Crashlytics", "NON-SENTRY crash: ${throwable.localizedMessage}")
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Log.d("Crashlytics", "NON-SENTRY start app")
}

fun pushPlayerError(error: PlayerError) {
    Log.e("Crashlytics", "NON-SENTRY Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
}
