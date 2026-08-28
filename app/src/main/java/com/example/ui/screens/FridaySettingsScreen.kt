package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.example.ui.components.ApiKeyOnboardingDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FridaySettingsScreen(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val isHotwordEnabled by viewModel.isHotwordEnabled.collectAsState()
    val isBackgroundListeningEnabled by viewModel.isBackgroundListeningEnabled.collectAsState()
    val isSaveHistoryEnabled by viewModel.isSaveHistoryEnabled.collectAsState()
    val assistantLanguage by viewModel.assistantLanguage.collectAsState()
    val malePitch by viewModel.malePitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val chatMessageCount by viewModel.chatMessageCount.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var testApiStatus by remember { mutableStateOf<String?>(null) }
    var isTestingApi by remember { mutableStateOf(false) }
    var isTestError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FridayDarkBg)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
            .testTag("friday_settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(FridayCyan.copy(alpha = 0.25f), FridayPurple.copy(alpha = 0.25f))
                        )
                    )
                    .border(BorderStroke(1.5.dp, FridayCyan), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = FridayCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Friday Ayarları",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayTextPrimary
                    )
                )
                Text(
                    text = "AI, Səs, Dil, Oyanma və Yaddaş İdarəetməsi",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FridayTextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 1. GEMINI AI KONFİQURASİYASI & API KEY
        // ==========================================
        Text(
            text = "SÜNİ İNTELLEKT & GEMINI API",
            style = MaterialTheme.typography.labelMedium.copy(
                color = FridayCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FridayCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("gemini_api_settings_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = FridayCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini 3.5 Flash Model",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                    }

                    // Status Badge
                    val hasKey = geminiApiKey.isNotBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasKey) FridayGreen.copy(alpha = 0.2f) else FridayAmber.copy(alpha = 0.2f),
                        border = BorderStroke(
                            0.5.dp,
                            if (hasKey) FridayGreen else FridayAmber
                        )
                    ) {
                        Text(
                            text = if (hasKey) "Aktiv Key" else "Açar Daxil Edilməyib",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasKey) FridayGreen else FridayAmber,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayKey = if (geminiApiKey.isNotBlank()) {
                    if (geminiApiKey.length > 12) {
                        "${geminiApiKey.take(7)}••••••••${geminiApiKey.takeLast(4)}"
                    } else {
                        "••••••••••••"
                    }
                } else {
                    "API Key quraşdırılmayıb (Offline ağıllı rejim aktivdir)"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FridayDarkSurface,
                    border = BorderStroke(0.5.dp, FridayBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Saxlanılmış API Açar:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FridayTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = displayKey,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (geminiApiKey.isNotBlank()) FridayCyan else FridayTextSecondary
                                )
                            )
                        }
                    }
                }

                // Test Feedback Status
                AnimatedVisibility(visible = testApiStatus != null) {
                    testApiStatus?.let { status ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTestError) FridayRed.copy(alpha = 0.15f) else FridayGreen.copy(alpha = 0.15f),
                            border = BorderStroke(
                                0.5.dp,
                                if (isTestError) FridayRed.copy(alpha = 0.5f) else FridayGreen.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTestError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isTestError) FridayRed else FridayGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isTestError) FridayRed else FridayGreen
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showApiKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FridayCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("change_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Edit Key",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (geminiApiKey.isNotBlank()) "Açarı Dəyiş" else "API Key Daxil Et",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (geminiApiKey.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                isTestingApi = true
                                testApiStatus = "Əlaqə yoxlanılır..."
                                isTestError = false
                                coroutineScope.launch {
                                    val res = viewModel.testGeminiConnection(geminiApiKey)
                                    isTestingApi = false
                                    res.onSuccess {
                                        testApiStatus = it
                                        isTestError = false
                                    }.onFailure {
                                        testApiStatus = it.message ?: "Əlaqə xətası"
                                        isTestError = true
                                    }
                                }
                            },
                            enabled = !isTestingApi,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayCyan),
                            border = BorderStroke(1.dp, FridayCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("test_api_connection_button")
                        ) {
                            if (isTestingApi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = FridayCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = "Test",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Əlaqəni Yoxla", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 2. KİŞİ SƏSİ & TTS PARAMETRLƏRİ
        // ==========================================
        Text(
            text = "SƏS & DANIŞIQ TONU (TTS)",
            style = MaterialTheme.typography.labelMedium.copy(
                color = FridayCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth().testTag("voice_pitch_speed_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Pitch (Səs Qalınlığı)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Pitch",
                            tint = FridayCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Səs Tonu (Qalınlıq / Kişi Səsi)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = FridayTextPrimary
                            )
                        )
                    }
                    Text(
                        text = "%.2fx".format(malePitch),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FridayCyan
                        )
                    )
                }

                Slider(
                    value = malePitch,
                    onValueChange = { viewModel.setMaleVoicePitch(it) },
                    valueRange = 0.60f..1.30f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = FridayCyan,
                        activeTrackColor = FridayCyan,
                        inactiveTrackColor = FridayBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("pitch_slider")
                )

                // Pitch presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Dərin" to 0.75f,
                        "Jarvis" to 0.82f,
                        "Təbii" to 0.90f,
                        "Aydın" to 1.00f
                    ).forEach { (label, value) ->
                        val isSelected = kotlin.math.abs(malePitch - value) < 0.03f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) FridayCyan.copy(alpha = 0.2f) else FridayDarkSurface,
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected) FridayCyan else FridayBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setMaleVoicePitch(value) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) FridayCyan else FridayTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = FridayBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Speech Rate (Danışıq Sürəti)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = FridayPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Danışıq Sürəti",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = FridayTextPrimary
                            )
                        )
                    }
                    Text(
                        text = "%.2fx".format(speechRate),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FridayPurple
                        )
                    )
                }

                Slider(
                    value = speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.60f..1.80f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = FridayPurple,
                        activeTrackColor = FridayPurple,
                        inactiveTrackColor = FridayBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rate_slider")
                )

                // Rate presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Təmkinli" to 0.85f,
                        "Normal" to 1.00f,
                        "Sürətli" to 1.20f,
                        "Cəld" to 1.45f
                    ).forEach { (label, value) ->
                        val isSelected = kotlin.math.abs(speechRate - value) < 0.04f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) FridayPurple.copy(alpha = 0.2f) else FridayDarkSurface,
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected) FridayPurple else FridayBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setSpeechRate(value) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) FridayPurple else FridayTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test & Reset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetVoiceDefaults() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayTextSecondary),
                        border = BorderStroke(1.dp, FridayBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sıfırla", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val sample = when (assistantLanguage) {
                                "EN" -> "Hello! Voice pitch and rate configured. Friday is ready."
                                "RU" -> "Приветствую! Настройки голоса успешно обновлены."
                                "TR" -> "Merhaba! Ses ayarları başarıyla uygulandı."
                                else -> "Salam! Səs tonu və sürət parametrləri tətbiq edildi. Friday xidmətinizdədir."
                            }
                            viewModel.testVoiceSettings(sample)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) FridayPurple else FridayCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Test Voice",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpeaking) "Dayandır" else "Səsi Test Et",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 3. DİL SEÇİMİ (MULTILINGUAL PREFERENCE)
        // ==========================================
        Text(
            text = "KÖMƏKÇİ DİLİ",
            style = MaterialTheme.typography.labelMedium.copy(
                color = FridayCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth().testTag("language_selector_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Friday bütün dilləri başa düşür və danışır. İlkin ünsiyyət dilini seçin:",
                    style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "AZ" to "🇦🇿 Azərbaycan",
                        "EN" to "🇬🇧 English",
                        "RU" to "🇷🇺 Русский",
                        "TR" to "🇹🇷 Türkçe"
                    ).forEach { (code, title) ->
                        val isSelected = assistantLanguage == code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) FridayCyan.copy(alpha = 0.25f) else FridayDarkSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FridayCyan else FridayBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setAssistantLanguage(code) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title.take(2), // flag emoji
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) FridayCyan else FridayTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 4. OYANMA & ARXA PLAN DİNLƏMƏSİ
        // ==========================================
        Text(
            text = "OYANMA SÖZÜ & ARXA PLAN DİNLƏMƏSİ",
            style = MaterialTheme.typography.labelMedium.copy(
                color = FridayCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth().testTag("hotword_background_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Hotword Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "'Hey Friday' / 'Friday' Oyanma İfadəsi",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Tətbiq açıq olanda 'Friday' və ya 'Hey Friday' deyərək köməkçini oyadın",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isHotwordEnabled,
                        onCheckedChange = { viewModel.toggleHotword(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FridayCyan,
                            checkedTrackColor = FridayCyan.copy(alpha = 0.4f),
                            uncheckedThumbColor = FridayTextSecondary,
                            uncheckedTrackColor = FridayBorder
                        ),
                        modifier = Modifier.testTag("hotword_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = FridayBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Background Service Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Arxa Planda Fasiləsiz Dinləmə",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBackgroundListeningEnabled) FridayGreen.copy(alpha = 0.2f) else FridayDarkSurface,
                                border = BorderStroke(0.5.dp, if (isBackgroundListeningEnabled) FridayGreen else FridayBorder)
                            ) {
                                Text(
                                    text = if (isBackgroundListeningEnabled) "Aktiv" else "Deaktiv",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isBackgroundListeningEnabled) FridayGreen else FridayTextSecondary,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tətbiqdən çıxsanız belə 'Friday' dedikdə əmrləri avtomatik yerinə yetirir (Bildiriş xidməti ilə).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isBackgroundListeningEnabled,
                        onCheckedChange = { viewModel.toggleBackgroundListening(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FridayGreen,
                            checkedTrackColor = FridayGreen.copy(alpha = 0.4f),
                            uncheckedThumbColor = FridayTextSecondary,
                            uncheckedTrackColor = FridayBorder
                        ),
                        modifier = Modifier.testTag("background_listening_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 5. DANISIQLAR VƏ YADDAŞ (CONVERSATION HISTORY)
        // ==========================================
        Text(
            text = "SÖHBƏT TARİXÇƏSİ & YADDAŞ",
            style = MaterialTheme.typography.labelMedium.copy(
                color = FridayCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = FridayDarkSurfaceContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth().testTag("chat_history_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Danışıqları Yaddaşda Saxla",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Friday ilə olan bütün söhbətlər daimi saxlanılır və tətbiqi bağlayanda silinmir.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isSaveHistoryEnabled,
                        onCheckedChange = { viewModel.toggleSaveHistory(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FridayCyan,
                            checkedTrackColor = FridayCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("save_history_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FridayDarkSurface,
                    border = BorderStroke(0.5.dp, FridayBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = FridayCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saxlanılmış Mesajlar:",
                                style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
                            )
                        }
                        Text(
                            text = "$chatMessageCount ədəd",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayRed),
                    border = BorderStroke(1.dp, FridayRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = FridayRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Söhbət Tarixçəsini Təmizlə", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = FridayCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Friday AI Assistant v1.0 • Bütün məlumatlar lokal saxlanılır",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FridayTextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }

    // Dialogs
    if (showApiKeyDialog) {
        ApiKeyOnboardingDialog(
            initialKey = geminiApiKey,
            isDismissible = true,
            onSaveKey = { newKey ->
                viewModel.saveGeminiApiKey(newKey)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Tarixçəni Təmizlə") },
            text = { Text("Bütün keçmiş söhbətlər və danışıq mesajları silinəcək. Bu əməliyyat geri qaytarıla bilməz.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FridayRed)
                ) {
                    Text("Bəli, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("İmtina", color = FridayTextSecondary)
                }
            },
            containerColor = FridayDarkSurface,
            titleContentColor = FridayTextPrimary,
            textContentColor = FridayTextSecondary
        )
    }
}
