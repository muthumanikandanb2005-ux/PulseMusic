package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.manikandan.common.Config
import com.pulse.music.R
import com.pulse.music.viewModel.SharedViewModel
import com.pulse.music.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class WaveformPillWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val controllerState by sharedViewModel.controllerState.collectAsState()
                val timelineState by sharedViewModel.timeline.collectAsState()
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()

                val timeText = if (timelineState.current > 0L) {
                    com.pulse.music.extension.formatDuration(timelineState.current)
                } else if (timelineState.total > 0L) {
                    com.pulse.music.extension.formatDuration(timelineState.total)
                } else if (screenDataState.nowPlayingTitle.isNotBlank()) {
                    "Live"
                } else {
                    "Pulse"
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(ImageProvider(R.drawable.widget_pill_surface))
                            .cornerRadius(999.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Soundwave bars in cyan/blue
                        Row(
                            modifier = GlanceModifier.defaultWeight().height(26.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val waveHeights = listOf(
                                6, 10, 16, 12, 20, 24, 18, 14, 22, 16, 12, 18, 22, 26, 18, 14, 20, 12, 8, 16, 22, 14, 10, 6
                            )
                            waveHeights.forEachIndexed { i, h ->
                                if (i > 0) Spacer(GlanceModifier.width(2.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(2.dp)
                                        .height(h.dp)
                                        .background(ColorProvider(Color(0xFF38BDF8)))
                                        .cornerRadius(1.dp),
                                ) {}
                            }
                        }

                        Spacer(GlanceModifier.width(8.dp))

                        // Time pill badge + Play/Pause button
                        Box(
                            modifier = GlanceModifier
                                .background(ColorProvider(Color(0xFF1E1E24)))
                                .cornerRadius(999.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeText,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(GlanceModifier.width(4.dp))
                                Image(
                                    provider = if (controllerState.isPlaying) {
                                        ImageProvider(R.drawable.rounded_pause_24)
                                    } else {
                                        ImageProvider(R.drawable.rounded_play_arrow_24)
                                    },
                                    contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier = GlanceModifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(GlanceModifier.width(8.dp))

                        // Primary Action Button (Cyan / Light Blue circular play/next button)
                        Box(
                            modifier = GlanceModifier
                                .size(34.dp)
                                .background(ColorProvider(Color(0xFF38BDF8)))
                                .cornerRadius(999.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Next)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.rounded_play_arrow_24),
                                contentDescription = "Play",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
                                modifier = GlanceModifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@WaveformPillWidget.javaClass).forEach { glanceId ->
            this@WaveformPillWidget.update(context, glanceId)
        }
    }
}
