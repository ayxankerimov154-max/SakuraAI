package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FridayViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsOverlay(
    viewModel: FridayViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHotwordEnabled by viewModel.isHotwordEnabled.collectAsState()
    val malePitch by viewModel.malePitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    var testLanguage by remember { mutableStateOf("AZ") }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FridayDarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(FridayCyan.copy(alpha = 0.4f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("voice_settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(FridayCyan.copy(alpha = 0.2f), FridayPurple.copy(alpha = 0.2f))
                                )
                            )
                            .border(BorderStroke(1.dp, FridayCyan), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Settings",
                            tint = FridayCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Friday Səs & AI Ayarları",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Gemini Key, Səs Qalınlığı, Sürət & Oyanma",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("voice_settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bağla",
                        tint = FridayTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 0. Gemini API Key Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FridayCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("overlay_gemini_key_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = FridayCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini API Açar",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayTextPrimary
                                )
                            )
                        }

                        val hasKey = geminiApiKey.isNotBlank()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasKey) FridayGreen.copy(alpha = 0.2f) else FridayAmber.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, if (hasKey) FridayGreen else FridayAmber)
                        ) {
                            Text(
                                text = if (hasKey) "Yaddaşda var" else "Açarsız",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (hasKey) FridayGreen else FridayAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    val keyDisplay = if (geminiApiKey.isNotBlank()) {
                        "${geminiApiKey.take(6)}••••••••${geminiApiKey.takeLast(4)}"
                    } else {
                        "API açarı daxil edilməyib (Offline rejim)"
                    }
                    Text(
                        text = keyDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (geminiApiKey.isNotBlank()) FridayCyan else FridayTextSecondary,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showApiKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FridayCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("overlay_change_api_key_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (geminiApiKey.isNotBlank()) "API Açarını Dəyiş" else "API Açarı Daxil Et",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. 'Hey Friday' Wake-Word Toggle Section ---
            Card(
                colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isHotwordEnabled) FridayCyan.copy(alpha = 0.5f) else FridayBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hey_friday_activation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHotwordEnabled) FridayCyan.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = null,
                                    tint = if (isHotwordEnabled) FridayCyan else FridayTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "'Hey Friday' Oyanma İfadəsi",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = FridayTextPrimary
                                    )
                                )
                                Text(
                                    text = if (isHotwordEnabled) "Aktiv — Arxa fonda dinləyir" else "Deaktiv",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isHotwordEnabled) FridayGreen else FridayTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = isHotwordEnabled,
                            onCheckedChange = { viewModel.toggleHotword(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = FridayCyan,
                                uncheckedThumbColor = FridayTextSecondary,
                                uncheckedTrackColor = FridayDarkSurfaceHigh
                            ),
                            modifier = Modifier.testTag("hey_friday_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Aktiv olduqda telefon 'Hey Friday', 'Hey Fida' və ya 'Salam Friday' eşidən kimi avtomatik əmr qəbulu rejiminə keçir.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FridayTextSecondary,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hotword tags row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hey Friday", "Hey Fida", "Fida", "Salam Friday").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FridayDarkSurfaceHigh,
                                border = BorderStroke(0.5.dp, FridayCyan.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FridayCyan,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 2. Voice Pitch Adjustment ---
            Card(
                colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FridayBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_pitch_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = FridayCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Səs Tonu (Pitch - Kişi Profili)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FridayTextPrimary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FridayCyan.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, FridayCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", malePitch),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FridayCyan,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = malePitch,
                        onValueChange = { viewModel.setMaleVoicePitch(it) },
                        valueRange = 0.60f..1.30f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = FridayCyan,
                            activeTrackColor = FridayCyan,
                            inactiveTrackColor = FridayDarkSurfaceHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_pitch_slider")
                    )

                    // Preset Pitch Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val pitchPresets = listOf(
                            "Dərin (0.75x)" to 0.75f,
                            "Jarvis (0.82x)" to 0.82f,
                            "Təbii (0.90x)" to 0.90f,
                            "Aydın (1.00x)" to 1.00f
                        )
                        pitchPresets.forEach { (label, value) ->
                            val isSelected = Math.abs(malePitch - value) < 0.03f
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setMaleVoicePitch(value) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FridayCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = FridayCyan,
                                    containerColor = FridayDarkSurfaceHigh,
                                    labelColor = FridayTextSecondary
                                ),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) FridayCyan else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 3. Speech Rate / Speed Adjustment ---
            Card(
                colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FridayBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_speed_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = FridayPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Danışıq Sürəti (Speed Rate)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FridayTextPrimary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FridayPurple.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, FridayPurple.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fx", speechRate),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FridayPurple,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.60f..1.80f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = FridayPurple,
                            activeTrackColor = FridayPurple,
                            inactiveTrackColor = FridayDarkSurfaceHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_speed_slider")
                    )

                    // Preset Speed Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val speedPresets = listOf(
                            "Təmkinli (0.85x)" to 0.85f,
                            "Normal (1.00x)" to 0.98f,
                            "Sürətli (1.20x)" to 1.20f,
                            "Cəld (1.45x)" to 1.45f
                        )
                        speedPresets.forEach { (label, value) ->
                            val isSelected = Math.abs(speechRate - value) < 0.04f
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSpeechRate(value) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FridayPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = FridayPurple,
                                    containerColor = FridayDarkSurfaceHigh,
                                    labelColor = FridayTextSecondary
                                ),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) FridayPurple else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 4. Live Voice Testing & Presets ---
            Text(
                text = "Səs Test Nümunəsi:",
                style = MaterialTheme.typography.labelMedium.copy(color = FridayTextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = listOf("AZ" to "Azərbaycan", "EN" to "English", "RU" to "Русский", "TR" to "Türkçe")
                languages.forEach { (code, name) ->
                    val isSelected = testLanguage == code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) FridayCyan.copy(alpha = 0.2f) else FridayDarkSurfaceContainer,
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) FridayCyan else FridayBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { testLanguage = code }
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) FridayCyan else FridayTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Test Voice & Reset Defaults
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetVoiceDefaults() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayTextSecondary),
                    border = BorderStroke(1.dp, FridayBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_reset_defaults_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("İlkin Vəziyyət", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val samplePhrase = when (testLanguage) {
                            "EN" -> "Hello sir! Voice pitch and speech rate configured successfully. Friday is at your service."
                            "RU" -> "Приветствую, сэр! Настройки тона и скорости голоса успешно применены. Я готов к работе."
                            "TR" -> "Merhaba efendim! Ses tonu ve konuşma hızı başarıyla güncellendi. Friday hizmetinizde."
                            else -> "Salam cənab! Səs tonu və danışıq sürəti uğurla tənzimləndi. Friday xidmətinizdədir."
                        }
                        viewModel.testVoiceSettings(samplePhrase)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) FridayPurple else FridayCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("voice_test_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Test Voice",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpeaking) "Dayandır" else "Səsi Test Et",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyOnboardingDialog(
            initialKey = geminiApiKey,
            isDismissible = true,
            onSaveKey = { key ->
                viewModel.saveGeminiApiKey(key)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}
