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
import androidx.glance.unit.ColorProvider
import com.pulse.music.PulseMusicActivity
import com.pulse.music.R
import org.koin.core.component.KoinComponent

class GenreSelectorWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
                            .cornerRadius(24.dp)
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.round_folder_music_24),
                                contentDescription = "Genre",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.LightGray)),
                                modifier = GlanceModifier.size(16.dp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text = "Genre",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        Spacer(GlanceModifier.height(10.dp))

                        // Pop
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🎤",
                                style = TextStyle(fontSize = 13.sp),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "Pop",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }

                        // Rock (with [new] badge)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🎸",
                                style = TextStyle(fontSize = 13.sp),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "Rock",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(Color(0xFFEAB308)))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "new",
                                    style = TextStyle(
                                        color = ColorProvider(Color.Black),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }

                        // Electronic
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🎛️",
                                style = TextStyle(fontSize = 13.sp),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "Electronic",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }

                        // Jazz
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable(
                                    actionStartActivity<PulseMusicActivity>(),
                                    rippleOverride = R.drawable.no_ripple,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🎷",
                                style = TextStyle(fontSize = 13.sp),
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "Jazz",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(this@GenreSelectorWidget.javaClass).forEach { glanceId ->
            this@GenreSelectorWidget.update(context, glanceId)
        }
    }
}
