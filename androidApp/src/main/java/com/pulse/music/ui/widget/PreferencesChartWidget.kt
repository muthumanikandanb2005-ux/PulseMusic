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
import com.pulse.music.viewModel.SharedViewModel
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferencesChartWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()
    private val analyticsRepository by inject<AnalyticsRepository>()

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val screenData = sharedViewModel.nowPlayingScreenData.value
        val hasTrack = screenData.nowPlayingTitle.isNotBlank()
        val promptTitle = if (hasTrack) {
            "Now Playing"
        } else {
            "Preferences"
        }

        val promptSubtitle = if (hasTrack) {
            "${screenData.nowPlayingTitle}\n${screenData.artistName}"
        } else {
            "What kind of music\nare you looking for?"
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
                        // Header: Preferences + three dots
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = promptTitle,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            Image(
                                provider = ImageProvider(R.drawable.round_more_vert_24),
                                contentDescription = "More",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.Gray)),
                                modifier = GlanceModifier.size(16.dp),
                            )
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Equalizer / Frequency Bar Chart with Days of Week
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Bar chart columns
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                val barData = listOf(
                                    Pair(20.dp, Color(0xFF333338)),
                                    Pair(35.dp, Color(0xFFF59E0B)), // Orange
                                    Pair(48.dp, Color(0xFFEF4444)), // Red
                                    Pair(24.dp, Color(0xFF333338)),
                                    Pair(42.dp, Color(0xFF22C55E)), // Green
                                    Pair(46.dp, Color(0xFF06B6D4)), // Cyan
                                    Pair(36.dp, Color(0xFF333338)),
                                    Pair(44.dp, Color(0xFF3B82F6)), // Blue
                                    Pair(30.dp, Color(0xFF22C55E)), // Green
                                    Pair(22.dp, Color(0xFF333338)),
                                    Pair(38.dp, Color(0xFF22C55E)), // Green
                                    Pair(18.dp, Color(0xFF333338)),
                                    Pair(15.dp, Color(0xFF333338)),
                                )

                                barData.forEachIndexed { index, (heightDp, color) ->
                                    if (index > 0) Spacer(GlanceModifier.width(4.dp))
                                    Box(
                                        modifier = GlanceModifier
                                            .width(4.dp)
                                            .height(heightDp)
                                            .background(ColorProvider(color))
                                            .cornerRadius(2.dp),
                                    ) {}
                                }
                            }

                            Spacer(GlanceModifier.height(4.dp))

                            // Day labels
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S", "S", "M", "T", "W", "T", "S")
                                dayLetters.forEachIndexed { index, letter ->
                                    if (index > 0) Spacer(GlanceModifier.width(3.dp))
                                    Text(
                                        text = letter,
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF777777)),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    )
                                }
                            }
                        }

                        Spacer(GlanceModifier.height(6.dp))

                        // Real track or prompt Text
                        Text(
                            text = promptSubtitle,
                            style = TextStyle(
                                color = ColorProvider(Color.LightGray),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 2,
                        )

                        Spacer(GlanceModifier.defaultWeight())

                        // Bottom Actions: Genres chip + Search icon
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(Color(0xFF242428)))
                                    .cornerRadius(999.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable(
                                        actionStartActivity<PulseMusicActivity>(),
                                        rippleOverride = R.drawable.no_ripple,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Library ▾",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }

                            Spacer(GlanceModifier.defaultWeight())

                            Box(
                                modifier = GlanceModifier
                                    .size(34.dp)
                                    .background(ColorProvider(Color(0xFF242428)))
                                    .cornerRadius(999.dp)
                                    .clickable(
                                        actionStartActivity<PulseMusicActivity>(),
                                        rippleOverride = R.drawable.no_ripple,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_search_24),
                                    contentDescription = "Search",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                    modifier = GlanceModifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
