package com.example.ui.screens

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.SystemState
import com.example.ui.FridayViewModel
import com.example.ui.theme.*

@Composable
fun WidgetsScreen(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    val systemState by viewModel.systemState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FridayDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Telefon Ayarları & Widget-lər",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayTextPrimary
                    )
                )
                Text(
                    text = "Bütün widget-lər həm toxunma, həm də səsli əmrlərlə idarə olunur",
                    style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                )
            }

            IconButton(
                onClick = { viewModel.systemSettingsManager.refreshState() },
                modifier = Modifier
                    .size(40.dp)
                    .background(FridayDarkSurfaceContainer, CircleShape)
                    .testTag("refresh_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yenilə",
                    tint = FridayCyan
                )
            }
        }

        // --- Grid of Quick Toggle Widgets (Wi-Fi, Bluetooth, Flashlight) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wi-Fi Widget
            QuickToggleWidget(
                title = "Wi-Fi",
                subtitle = if (systemState.isWifiEnabled) "Aktivdir" else "Söndürülüb",
                isOn = systemState.isWifiEnabled,
                icon = Icons.Default.Wifi,
                voiceTip = "\"Wi-Fi aç\" / \"Wi-Fi söndür\"",
                onToggle = { viewModel.toggleWifi() },
                onLongClick = { viewModel.systemSettingsManager.openWifiSettings() },
                modifier = Modifier.weight(1f),
                testTag = "wifi_widget"
            )

            // Bluetooth Widget
            QuickToggleWidget(
                title = "Bluetooth",
                subtitle = if (systemState.isBluetoothEnabled) "Aktivdir" else "Söndürülüb",
                isOn = systemState.isBluetoothEnabled,
                icon = Icons.Default.Bluetooth,
                voiceTip = "\"Bluetooth aç\" / \"Bluetooth söndür\"",
                onToggle = { viewModel.toggleBluetooth() },
                onLongClick = { viewModel.systemSettingsManager.openBluetoothSettings() },
                modifier = Modifier.weight(1f),
                testTag = "bluetooth_widget"
            )
        }

        // Flashlight & Battery Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flashlight Widget
            QuickToggleWidget(
                title = "Fənər (İşıq)",
                subtitle = if (systemState.isFlashlightOn) "Yanıb" else "Sönüb",
                isOn = systemState.isFlashlightOn,
                icon = Icons.Default.FlashlightOn,
                activeColor = FridayAmber,
                voiceTip = "\"Fənəri yandır\" / \"Fənəri söndür\"",
                onToggle = { viewModel.toggleFlashlight() },
                modifier = Modifier.weight(1f),
                testTag = "flashlight_widget"
            )

            // Battery Status Widget
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = FridayDarkSurfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FridayGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (systemState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = FridayGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "%${systemState.batteryPercent}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Batareya",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = FridayTextPrimary
                        )
                    )
                    Text(
                        text = if (systemState.isCharging) "Zaryadka yığır" else "Enerji normal",
                        style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                    )
                }
            }
        }

        // --- Volume Sliders Widget ---
        VolumeControlWidget(
            systemState = systemState,
            onMediaChange = { viewModel.setMediaVolume(it) },
            onRingChange = { viewModel.setRingVolume(it) },
            onAlarmChange = { viewModel.setAlarmVolume(it) },
            onMuteToggle = {
                if (systemState.mediaVolumePercent > 0) viewModel.setMediaVolume(0)
                else viewModel.setMediaVolume(70)
            },
            onOpenSettings = { viewModel.systemSettingsManager.openSoundSettings() }
        )

        // --- Screen Brightness Widget ---
        BrightnessControlWidget(
            brightnessPercent = systemState.brightnessPercent,
            onBrightnessChange = { viewModel.setBrightness(it) },
            onOpenSettings = { viewModel.systemSettingsManager.openDisplaySettings() }
        )

        // --- Ringer Mode Selector Widget ---
        RingerModeWidget(
            currentMode = systemState.ringerMode,
            onModeSelect = { viewModel.setRingerMode(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun QuickToggleWidget(
    title: String,
    subtitle: String,
    isOn: Boolean,
    icon: ImageVector,
    voiceTip: String,
    onToggle: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    activeColor: Color = FridayCyan,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isOn) activeColor.copy(alpha = 0.15f) else FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOn) activeColor.copy(alpha = 0.5f) else FridayBorder
        ),
        modifier = modifier
            .testTag(testTag)
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isOn) activeColor else FridayDarkSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isOn) Color.Black else FridayTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Switch(
                    checked = isOn,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = activeColor,
                        checkedTrackColor = activeColor.copy(alpha = 0.4f),
                        uncheckedThumbColor = FridayTextSecondary,
                        uncheckedTrackColor = FridayDarkSurfaceHigh
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FridayTextPrimary
                )
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isOn) activeColor else FridayTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = voiceTip,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FridayTextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun VolumeControlWidget(
    systemState: SystemState,
    onMediaChange: (Int) -> Unit,
    onRingChange: (Int) -> Unit,
    onAlarmChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("volume_control_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FridayNeonBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (systemState.mediaVolumePercent > 0) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = FridayNeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Səs Səviyyələri Widget",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Səsli əmr: \"Səsi 70% et\", \"Səsi artır\"",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayCyan)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Səs Parametrləri",
                        tint = FridayTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Media Volume Slider
            VolumeSliderRow(
                label = "Media & Musiqi",
                valuePercent = systemState.mediaVolumePercent,
                icon = Icons.Default.MusicNote,
                onValueChange = onMediaChange,
                testTag = "media_volume_slider"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ringtone Volume Slider
            VolumeSliderRow(
                label = "Zəng Səsi",
                valuePercent = systemState.ringVolumePercent,
                icon = Icons.Default.Notifications,
                onValueChange = onRingChange,
                testTag = "ring_volume_slider"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alarm Volume Slider
            VolumeSliderRow(
                label = "Zəngli Saat / Zəng",
                valuePercent = systemState.alarmVolumePercent,
                icon = Icons.Default.Alarm,
                onValueChange = onAlarmChange,
                testTag = "alarm_volume_slider"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Volume Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMuteToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayTextPrimary)
                ) {
                    Text(if (systemState.mediaVolumePercent == 0) "Səsi Aç" else "Səssiz (Mute)", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onMediaChange(50); onRingChange(50) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayCyan)
                ) {
                    Text("50%", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onMediaChange(100); onRingChange(100) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayCyan)
                ) {
                    Text("100% (Maks)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VolumeSliderRow(
    label: String,
    valuePercent: Int,
    icon: ImageVector,
    onValueChange: (Int) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FridayTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                )
            }
            Text(
                text = "%$valuePercent",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FridayCyan
                )
            )
        }

        Slider(
            value = valuePercent.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = FridayCyan,
                activeTrackColor = FridayCyan,
                inactiveTrackColor = FridayDarkSurfaceHigh
            )
        )
    }
}

@Composable
fun BrightnessControlWidget(
    brightnessPercent: Int,
    onBrightnessChange: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("brightness_control_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FridayAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness6,
                            contentDescription = null,
                            tint = FridayAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ekran Parlaqlığı Widget",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Səsli əmr: \"Parlaqlığı artır\", \"Parlaqlığı 100% et\"",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayAmber)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ekran Parametrləri",
                        tint = FridayTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Parlaqlıq Səviyyəsi",
                    style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                )
                Text(
                    text = "%$brightnessPercent",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayAmber
                    )
                )
            }

            Slider(
                value = brightnessPercent.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 5f..100f,
                modifier = Modifier.testTag("brightness_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = FridayAmber,
                    activeTrackColor = FridayAmber,
                    inactiveTrackColor = FridayDarkSurfaceHigh
                )
            )

            // Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25, 50, 75, 100).forEach { preset ->
                    OutlinedButton(
                        onClick = { onBrightnessChange(preset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (brightnessPercent == preset) FridayAmber.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (brightnessPercent == preset) FridayAmber else FridayTextSecondary
                        )
                    ) {
                        Text("$preset%", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RingerModeWidget(
    currentMode: Int,
    onModeSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ringer_mode_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Səs Rejimi Profil",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FridayTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Normal
                FilterChip(
                    selected = currentMode == AudioManager.RINGER_MODE_NORMAL,
                    onClick = { onModeSelect(AudioManager.RINGER_MODE_NORMAL) },
                    label = { Text("Normal", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FridayCyan.copy(alpha = 0.2f),
                        selectedLabelColor = FridayCyan
                    )
                )

                // Vibrate
                FilterChip(
                    selected = currentMode == AudioManager.RINGER_MODE_VIBRATE,
                    onClick = { onModeSelect(AudioManager.RINGER_MODE_VIBRATE) },
                    label = { Text("Vibrasiya", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FridayAmber.copy(alpha = 0.2f),
                        selectedLabelColor = FridayAmber
                    )
                )

                // Silent
                FilterChip(
                    selected = currentMode == AudioManager.RINGER_MODE_SILENT,
                    onClick = { onModeSelect(AudioManager.RINGER_MODE_SILENT) },
                    label = { Text("Səssiz", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FridayPurple.copy(alpha = 0.2f),
                        selectedLabelColor = FridayPurple
                    )
                )
            }
        }
    }
}
