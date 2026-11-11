package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.glance.layout.ContentScale
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

class VolumeTrackOrbWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    private val sharedViewModel by inject<SharedViewModel>()
    private val serviceScope by inject<CoroutineScope>(named(Config.SERVICE_SCOPE))

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val controllerState by sharedViewModel.controllerState.collectAsState()
                val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsState()
                val timelineState by sharedViewModel.timeline.collectAsState()

                var artwork by remember { mutableStateOf<Bitmap?>(null) }
                val thumbUrl by remember { derivedStateOf { screenDataState.thumbnailURL } }

                LaunchedEffect(thumbUrl) {
                    artwork = context.loadBitmap(thumbUrl, thumbUrl + "ORB", 180)
                }

                // Query device media volume
                val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
                val volumePercent = remember(controllerState) {
                    val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
                    val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                    if (max > 0) ((current.toFloat() / max.toFloat()) * 100).toInt() else 65
                }

                val title = if (screenDataState.nowPlayingTitle.isNotBlank()) screenDataState.nowPlayingTitle else "Pulse Music"
                val artist = if (screenDataState.artistName.isNotBlank()) screenDataState.artistName else "Tap to play"

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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Top row: Volume label + Cast icon
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.round_volume_up_24),
                                contentDescription = "Volume",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.LightGray)),
                                modifier = GlanceModifier.size(14.dp),
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                text = "Volume",
                                style = TextStyle(
                                    color = ColorProvider(Color.LightGray),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            Image(
                                provider = ImageProvider(R.drawable.round_cast_24),
                                contentDescription = "Cast",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.LightGray)),
                                modifier = GlanceModifier.size(16.dp),
                            )
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Center: Volume percentage and Glowing Orb Chain
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = GlanceModifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "$volumePercent%",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )

                            Spacer(GlanceModifier.height(10.dp))

                            // Glowing Spheres / Orbs row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = GlanceModifier.fillMaxWidth(),
                            ) {
                                Box(modifier = GlanceModifier.size(6.dp).background(ColorProvider(Color(0x334ADE80))).cornerRadius(999.dp)) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(modifier = GlanceModifier.size(10.dp).background(ColorProvider(Color(0x554ADE80))).cornerRadius(999.dp)) {}
                                Spacer(GlanceModifier.width(5.dp))
                                Box(modifier = GlanceModifier.size(16.dp).background(ColorProvider(Color(0x884ADE80))).cornerRadius(999.dp)) {}
                                Spacer(GlanceModifier.width(6.dp))

                                // Central glowing orb
                                Box(
                                    modifier = GlanceModifier
                                        .size(34.dp)
                                        .background(ImageProvider(R.drawable.widget_orb_center))
                                        .cornerRadius(999.dp),
                                ) {}

                                Spacer(GlanceModifier.width(6.dp))
                                Box(modifier = GlanceModifier.size(16.dp).background(ColorProvider(Color(0x884ADE80))).cornerRadius(999.dp)) {}
                                Spacer(GlanceModifier.width(5.dp))
                                Box(modifier = GlanceModifier.size(10.dp).background(ColorProvider(Color(0x554ADE80))).cornerRadius(999.dp)) {}
                                Spacer(GlanceModifier.width(4.dp))
                                Box(modifier = GlanceModifier.size(6.dp).background(ColorProvider(Color(0x334ADE80))).cornerRadius(999.dp)) {}
                            }
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Bottom: Track artwork + Title + Artist + Progress line
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(rippleOverride = R.drawable.no_ripple) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Thumbnail
                            Box(
                                modifier = GlanceModifier
                                    .size(40.dp)
                                    .cornerRadius(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (artwork != null) {
                                    Image(
                                        provider = ImageProvider(artwork!!),
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                                    )
                                } else {
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxSize()
                                            .background(ColorProvider(Color(0xFF26262B)))
                                            .cornerRadius(8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            provider = ImageProvider(R.drawable.round_music_note_24),
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFA788FA))),
                                            modifier = GlanceModifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(GlanceModifier.height(6.dp))

                            Text(
                                text = title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )

                            Text(
                                text = artist,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFAAAAAA)),
                                    fontSize = 10.sp,
                                ),
                            )

                            Spacer(GlanceModifier.height(6.dp))

                            // Progress track bar
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(ColorProvider(Color(0xFF2E2E33)))
                                    .cornerRadius(2.dp),
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(70.dp)
                                        .height(3.dp)
                                        .background(ColorProvider(Color(0xFF888888)))
                                        .cornerRadius(2.dp),
                                ) {}
                            }

                            Spacer(GlanceModifier.height(4.dp))

                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = com.pulse.music.extension.formatDuration(timelineState.current),
                                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp),
                                )
                                Spacer(GlanceModifier.defaultWeight())
                                Text(
                                    text = com.pulse.music.extension.formatDuration(timelineState.total),
                                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 9.sp),
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
        manager.getGlanceIds(this@VolumeTrackOrbWidget.javaClass).forEach { glanceId ->
            this@VolumeTrackOrbWidget.update(context, glanceId)
        }
    }
}
