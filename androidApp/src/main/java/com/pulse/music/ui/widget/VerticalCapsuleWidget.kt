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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
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

class VerticalCapsuleWidget : GlanceAppWidget(), KoinComponent {
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
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ImageProvider(R.drawable.widget_capsule_surface))
                            .cornerRadius(999.dp)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Home icon button
                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .background(ImageProvider(R.drawable.widget_circle_button))
                                .cornerRadius(999.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.round_home_24),
                                contentDescription = "Home",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier.size(20.dp),
                            )
                        }

                        Spacer(GlanceModifier.height(10.dp))

                        // Fast Forward / Skip Next button
                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .background(ImageProvider(R.drawable.widget_circle_button))
                                .cornerRadius(999.dp)
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.Next)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.rounded_fast_forward_24),
                                contentDescription = "Next",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier.size(20.dp),
                            )
                        }

                        Spacer(GlanceModifier.height(10.dp))

                        // Primary Music Button (White circular button with dark music note)
                        Box(
                            modifier = GlanceModifier
                                .size(42.dp)
                                .background(ColorProvider(Color.White))
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
                                    ImageProvider(R.drawable.round_music_note_24)
                                },
                                contentDescription = if (controllerState.isPlaying) "Pause" else "Play",
                                colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF16161A))),
                                modifier = GlanceModifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@VerticalCapsuleWidget.javaClass).forEach { glanceId ->
            this@VerticalCapsuleWidget.update(context, glanceId)
        }
    }
}
