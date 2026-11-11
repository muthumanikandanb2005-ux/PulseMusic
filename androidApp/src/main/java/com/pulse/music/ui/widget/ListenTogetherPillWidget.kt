package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
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
import com.pulse.music.PulseMusicActivity
import com.pulse.music.R
import org.koin.core.component.KoinComponent

class ListenTogetherPillWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
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
                            .padding(horizontal = 12.dp)
                            .clickable(
                                actionStartActivity<PulseMusicActivity>(),
                                rippleOverride = R.drawable.no_ripple,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Avatar group (3 overlapping circles with soft colors)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(ColorProvider(Color(0xFF38BDF8)))
                                    .cornerRadius(999.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_groups_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
                                    modifier = GlanceModifier.size(14.dp),
                                )
                            }
                            Spacer(GlanceModifier.width(2.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(ColorProvider(Color(0xFFA788FA)))
                                    .cornerRadius(999.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_music_note_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
                                    modifier = GlanceModifier.size(14.dp),
                                )
                            }
                            Spacer(GlanceModifier.width(2.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(ColorProvider(Color(0xFFFBBF24)))
                                    .cornerRadius(999.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_headphones_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
                                    modifier = GlanceModifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(GlanceModifier.width(10.dp))

                        // Waveform visualization bars
                        Row(
                            modifier = GlanceModifier.defaultWeight().height(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val waveHeights = listOf(6, 12, 18, 10, 16, 22, 14, 8, 20, 14, 18, 10, 15, 8, 12, 6)
                            waveHeights.forEachIndexed { i, h ->
                                if (i > 0) Spacer(GlanceModifier.width(2.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(2.dp)
                                        .height(h.dp)
                                        .background(ColorProvider(Color(0xFFDDDDDD)))
                                        .cornerRadius(1.dp),
                                ) {}
                            }
                        }

                        Spacer(GlanceModifier.width(10.dp))

                        // Group Listen Together Button (circular)
                        Box(
                            modifier = GlanceModifier
                                .size(34.dp)
                                .background(ColorProvider(Color.White))
                                .cornerRadius(999.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.round_groups_24),
                                contentDescription = "Listen Together",
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
        manager.getGlanceIds(this@ListenTogetherPillWidget.javaClass).forEach { glanceId ->
            this@ListenTogetherPillWidget.update(context, glanceId)
        }
    }
}
