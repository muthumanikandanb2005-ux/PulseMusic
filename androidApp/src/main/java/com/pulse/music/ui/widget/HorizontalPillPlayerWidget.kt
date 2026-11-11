package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
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
import androidx.glance.unit.ColorProvider
import com.manikandan.domain.mediaservice.handler.RepeatState
import com.manikandan.common.Config
import com.pulse.music.PulseMusicActivity
import com.pulse.music.R
import com.pulse.music.viewModel.SharedViewModel
import com.pulse.music.viewModel.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class HorizontalPillPlayerWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val controllerState by sharedViewModel.controllerState.collectAsState()

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
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Music note / App button
                        Image(
                            provider = ImageProvider(R.drawable.round_music_note_24),
                            contentDescription = "Pulse Music",
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFA788FA))),
                            modifier = GlanceModifier
                                .size(30.dp)
                                .padding(4.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Rewind / -5s
                        Image(
                            provider = ImageProvider(R.drawable.rounded_fast_rewind_24),
                            contentDescription = "Rewind",
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFDDDDDD))),
                            modifier = GlanceModifier
                                .size(30.dp)
                                .padding(4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Backward)
                                },
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Shuffle toggle
                        Image(
                            provider = ImageProvider(R.drawable.baseline_shuffle_24),
                            contentDescription = "Shuffle",
                            colorFilter = ColorFilter.tint(
                                ColorProvider(
                                    if (controllerState.isShuffle) Color(0xFFA788FA) else Color(0xFF888888)
                                )
                            ),
                            modifier = GlanceModifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Shuffle)
                                },
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Previous
                        Image(
                            provider = ImageProvider(R.drawable.rounded_skip_previous_24),
                            contentDescription = "Previous",
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                            modifier = GlanceModifier
                                .size(32.dp)
                                .padding(4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Previous)
                                },
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Play / Pause disc
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .background(ImageProvider(R.drawable.widget_circle_button))
                                .cornerRadius(999.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = if (controllerState.isPlaying) {
                                    ImageProvider(R.drawable.rounded_pause_24)
                                } else {
                                    ImageProvider(R.drawable.rounded_play_arrow_24)
                                },
                                contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier.size(20.dp),
                            )
                        }

                        Spacer(GlanceModifier.width(4.dp))

                        // Next
                        Image(
                            provider = ImageProvider(R.drawable.rounded_skip_next_24),
                            contentDescription = "Next",
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                            modifier = GlanceModifier
                                .size(32.dp)
                                .padding(4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Next)
                                },
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Repeat toggle
                        Image(
                            provider = ImageProvider(R.drawable.baseline_repeat_24),
                            contentDescription = "Repeat",
                            colorFilter = ColorFilter.tint(
                                ColorProvider(
                                    if (controllerState.repeatState !is RepeatState.None) Color(0xFFA788FA) else Color(0xFF888888)
                                )
                            ),
                            modifier = GlanceModifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Repeat)
                                },
                        )

                        Spacer(GlanceModifier.width(4.dp))

                        // Volume / Audio indicator
                        Image(
                            provider = ImageProvider(R.drawable.round_volume_up_24),
                            contentDescription = "Volume",
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFDDDDDD))),
                            modifier = GlanceModifier
                                .size(28.dp)
                                .padding(4.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@HorizontalPillPlayerWidget.javaClass).forEach { glanceId ->
            this@HorizontalPillPlayerWidget.update(context, glanceId)
        }
    }
}
