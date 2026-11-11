package com.pulse.music.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.manikandan.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

object PulseWidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastUpdateMs = AtomicLong(0L)

    fun updateAll(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateMs.get() < 1200L) {
            return // Throttle to at most once per 1200ms to avoid Binder IPC saturation
        }
        lastUpdateMs.set(now)

        scope.launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@launch

                fun hasWidget(receiverClass: Class<*>): Boolean {
                    return try {
                        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, receiverClass))
                        ids != null && ids.isNotEmpty()
                    } catch (e: Exception) {
                        false
                    }
                }

                if (hasWidget(HorizontalPillPlayerWidgetReceiver::class.java)) {
                    HorizontalPillPlayerWidget().updateAll(context)
                }
                if (hasWidget(WaveformPillWidgetReceiver::class.java)) {
                    WaveformPillWidget().updateAll(context)
                }
                if (hasWidget(VolumeTrackOrbWidgetReceiver::class.java)) {
                    VolumeTrackOrbWidget().updateAll(context)
                }
                if (hasWidget(VerticalCapsuleWidgetReceiver::class.java)) {
                    VerticalCapsuleWidget().updateAll(context)
                }
                if (hasWidget(DeviceBatteryWidgetReceiver::class.java)) {
                    DeviceBatteryWidget().updateAll(context)
                }
                if (hasWidget(FavoritesTrendingWidgetReceiver::class.java)) {
                    FavoritesTrendingWidget().updateAll(context)
                }
                if (hasWidget(PreferencesChartWidgetReceiver::class.java)) {
                    PreferencesChartWidget().updateAll(context)
                }
                if (hasWidget(ListeningStatisticWidgetReceiver::class.java)) {
                    ListeningStatisticWidget().updateAll(context)
                }
                if (hasWidget(GenreSelectorWidgetReceiver::class.java)) {
                    GenreSelectorWidget().updateAll(context)
                }
                if (hasWidget(ListenTogetherPillWidgetReceiver::class.java)) {
                    ListenTogetherPillWidget().updateAll(context)
                }
                if (hasWidget(TurntableWidgetReceiver::class.java)) {
                    TurntableWidget().updateAll(context)
                }
                if (hasWidget(PlaylistsWidgetReceiver::class.java)) {
                    PlaylistsWidget().updateAll(context)
                }
                if (hasWidget(ListeningInsightsWidgetReceiver::class.java)) {
                    ListeningInsightsWidget().updateAll(context)
                }
            } catch (e: Exception) {
                Logger.w("PulseWidgetUpdater", "Failed to update widgets: ${e.message}")
            }
        }
    }
}
