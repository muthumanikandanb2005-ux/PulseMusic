package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import com.manikandan.domain.repository.AnalyticsRepository
import com.pulse.music.PulseMusicActivity
import com.pulse.music.R
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ListeningStatisticWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val analyticsRepository by inject<AnalyticsRepository>()

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val totalSeconds = try {
            analyticsRepository.getTotalListeningTimeInSeconds().firstOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
        val totalEvents = try {
            analyticsRepository.getTotalPlaybackEventCount().firstOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val timeBadge = when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            totalEvents > 0 -> "${totalEvents}p"
            else -> "0h"
        }

        val timeSummary = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            totalEvents > 0 -> "$totalEvents tracks"
            else -> "Pulse Music"
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ImageProvider(R.drawable.widget_card_surface))
                            .cornerRadius(28.dp)
                            .padding(14.dp)
                            .clickable(
                                actionStartActivity<PulseMusicActivity>(),
                                rippleOverride = R.drawable.no_ripple,
                            ),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // Header: 🎵 Statistic with real dynamic listening badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🎵",
                                style = TextStyle(fontSize = 12.sp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text = "Statistic",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            // Dynamic listening badge
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(Color.White))
                                    .cornerRadius(999.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = timeBadge,
                                    style = TextStyle(
                                        color = ColorProvider(Color.Black),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Center: Amber/Yellow Trend Line Chart visualization
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Graph trend curve representation
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val trendSteps = listOf(8, 14, 22, 18, 12, 36, 32, 28, 14, 20)
                                trendSteps.forEachIndexed { i, h ->
                                    if (i > 0) Spacer(GlanceModifier.width(6.dp))
                                    Box(
                                        modifier = GlanceModifier
                                            .width(4.dp)
                                            .height(h.dp)
                                            .background(
                                                ColorProvider(
                                                    if (h >= 32) Color(0xFFFBBF24) else Color(0x99F59E0B)
                                                )
                                            )
                                            .cornerRadius(2.dp),
                                    ) {}
                                }
                            }

                            Spacer(GlanceModifier.height(4.dp))

                            // Dashed baseline
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ColorProvider(Color(0xFF333338))),
                            ) {}
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Bottom: Activity icon + Real Pulse listening time legend
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(24.dp)
                                    .background(ColorProvider(Color(0xFF26262B)))
                                    .cornerRadius(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_trending_up_24),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.LightGray)),
                                    modifier = GlanceModifier.size(12.dp),
                                )
                            }

                            Spacer(GlanceModifier.width(8.dp))

                            // Yellow dot + Real time
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .background(ColorProvider(Color(0xFFFBBF24)))
                                    .cornerRadius(999.dp),
                            ) {}
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                text = timeSummary,
                                style = TextStyle(
                                    color = ColorProvider(Color.LightGray),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
