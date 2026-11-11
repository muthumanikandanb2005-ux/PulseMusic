package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.unit.ColorProvider
import com.pulse.music.PulseMusicActivity
import com.pulse.music.R
import com.pulse.music.viewModel.SharedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FavoritesTrendingWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()
                val currentTitle = screenDataState.nowPlayingTitle.ifBlank { "Pulse Top Pick" }
                val currentArtist = screenDataState.artistName.ifBlank { "Pulse Music" }
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Header: 🔅 Favorites
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🔅",
                                style = TextStyle(fontSize = 12.sp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text = "Favorites",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        Spacer(GlanceModifier.height(10.dp))

                        // Visual timeline blocks (horizontal multi-colored segments)
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(42.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFFEF4444))) // Red
                                        .cornerRadius(4.dp),
                                ) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(52.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFF22C55E))) // Green
                                        .cornerRadius(4.dp),
                                ) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(36.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFF06B6D4))) // Cyan
                                        .cornerRadius(4.dp),
                                ) {}
                            }

                            Spacer(GlanceModifier.height(4.dp))

                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(28.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFFF59E0B))) // Yellow
                                        .cornerRadius(4.dp),
                                ) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(62.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFF3B82F6))) // Blue
                                        .cornerRadius(4.dp),
                                ) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .width(40.dp)
                                        .height(10.dp)
                                        .background(ColorProvider(Color(0xFFF59E0B))) // Orange
                                        .cornerRadius(4.dp),
                                ) {}
                            }
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        // Category dots legend (Pop, Rock, Electronic, Jazz)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Box(modifier = GlanceModifier.size(5.dp).background(ColorProvider(Color(0xFF22C55E))).cornerRadius(999.dp)) {}
                            Spacer(GlanceModifier.width(3.dp))
                            Text(text = "Pop", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp))

                            Spacer(GlanceModifier.width(8.dp))
                            Box(modifier = GlanceModifier.size(5.dp).background(ColorProvider(Color(0xFFEF4444))).cornerRadius(999.dp)) {}
                            Spacer(GlanceModifier.width(3.dp))
                            Text(text = "Rock", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp))

                            Spacer(GlanceModifier.width(8.dp))
                            Box(modifier = GlanceModifier.size(5.dp).background(ColorProvider(Color(0xFF3B82F6))).cornerRadius(999.dp)) {}
                            Spacer(GlanceModifier.width(3.dp))
                            Text(text = "Electronic", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp))

                            Spacer(GlanceModifier.width(8.dp))
                            Box(modifier = GlanceModifier.size(5.dp).background(ColorProvider(Color(0xFFF59E0B))).cornerRadius(999.dp)) {}
                            Spacer(GlanceModifier.width(3.dp))
                            Text(text = "Jazz", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp))
                        }

                        Spacer(GlanceModifier.height(14.dp))

                        // Trending Albums Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            // Stacked mini thumbnails
                            Box(
                                modifier = GlanceModifier
                                    .size(36.dp)
                                    .background(ColorProvider(Color(0xFF2E2E36)))
                                    .cornerRadius(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.round_trending_up_24),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(18.dp),
                                )
                            }

                            Spacer(GlanceModifier.width(10.dp))

                            Column {
                                Text(
                                    text = "PULSE TRENDING",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF888888)),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "Hits",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(10.dp))

                        // Current Pulse Track
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(Color(0xFF1A1A20)))
                                .cornerRadius(12.dp)
                                .padding(8.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(34.dp)
                                    .background(ColorProvider(Color(0xFF383842)))
                                    .cornerRadius(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "💿", style = TextStyle(fontSize = 16.sp))
                            }
                            Spacer(GlanceModifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentTitle,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = currentArtist,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color.Gray),
                                        fontSize = 9.sp,
                                    ),
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(6.dp))

                        // Pulse Trending Pick
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(Color(0xFF1A1A20)))
                                .cornerRadius(12.dp)
                                .padding(8.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(34.dp)
                                    .background(ColorProvider(Color(0xFF383842)))
                                    .cornerRadius(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "🎵", style = TextStyle(fontSize = 16.sp))
                            }
                            Spacer(GlanceModifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Illuminati",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "Sushin Shyam",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(Color.Gray),
                                        fontSize = 9.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@FavoritesTrendingWidget.javaClass).forEach { glanceId ->
            this@FavoritesTrendingWidget.update(context, glanceId)
        }
    }
}
