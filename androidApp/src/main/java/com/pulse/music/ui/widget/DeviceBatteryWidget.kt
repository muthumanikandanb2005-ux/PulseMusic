package com.pulse.music.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
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

class DeviceBatteryWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val activeDeviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    val bt = outputs?.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    }
                    val wired = outputs?.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                    }
                    bt?.productName?.toString()?.takeIf { it.isNotBlank() }
                        ?: wired?.productName?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Pulse Audio"
                } else {
                    "Pulse Audio"
                }

                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 1..100 } ?: 82

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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header: Device Name + Connected status
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeDeviceName,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            Text(
                                text = "Connected",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF22C55E)), // Soft Green
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }

                        Spacer(GlanceModifier.defaultWeight())

                        // Center: Dual Earbuds with Arc Rings and Battery %
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Left Earbud with Arc Gauge
                            Box(
                                modifier = GlanceModifier
                                    .size(54.dp)
                                    .background(ColorProvider(Color(0x184ADE80)))
                                    .cornerRadius(999.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_earbud_left),
                                    contentDescription = "Left Earbud",
                                    modifier = GlanceModifier.size(34.dp),
                                )
                            }

                            Spacer(GlanceModifier.width(18.dp))

                            // Battery Percentage in Center
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$batteryLevel%",
                                    style = TextStyle(
                                        color = ColorProvider(Color.White),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(GlanceModifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        provider = ImageProvider(R.drawable.round_battery_full_24),
                                        contentDescription = "Battery",
                                        colorFilter = ColorFilter.tint(ColorProvider(Color.Gray)),
                                        modifier = GlanceModifier.size(12.dp),
                                    )
                                    Spacer(GlanceModifier.width(4.dp))
                                    Text(
                                        text = "battery",
                                        style = TextStyle(
                                            color = ColorProvider(Color.Gray),
                                            fontSize = 10.sp,
                                        ),
                                    )
                                }
                            }

                            Spacer(GlanceModifier.width(18.dp))

                            // Right Earbud with Arc Gauge
                            Box(
                                modifier = GlanceModifier
                                    .size(54.dp)
                                    .background(ColorProvider(Color(0x184ADE80)))
                                    .cornerRadius(999.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.widget_earbud_right),
                                    contentDescription = "Right Earbud",
                                    modifier = GlanceModifier.size(34.dp),
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
        manager.getGlanceIds(this@DeviceBatteryWidget.javaClass).forEach { glanceId ->
            this@DeviceBatteryWidget.update(context, glanceId)
        }
    }
}
