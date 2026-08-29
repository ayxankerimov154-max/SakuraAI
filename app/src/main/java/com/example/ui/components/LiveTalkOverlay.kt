package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.FridayViewModel
import com.example.ui.LiveTalkPhase
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveTalkOverlay(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    val isLiveTalkActive by viewModel.isLiveTalkActive.collectAsState()
    val liveTalkPhase by viewModel.liveTalkPhase.collectAsState()
    val isMuted by viewModel.isLiveTalkMuted.collectAsState()
    val durationSeconds by viewModel.liveTalkDurationSeconds.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val partialText by viewModel.partialSpeechText.collectAsState()
    val latestFridayReply by viewModel.liveTalkLatestFridayReply.collectAsState()
    val latestUserPrompt by viewModel.liveTalkLatestUserPrompt.collectAsState()
    val currentLanguage by viewModel.assistantLanguage.collectAsState()

    var showQuickVoiceSettings by remember { mutableStateOf(false) }

    if (!isLiveTalkActive) return

    val formattedDuration = remember(durationSeconds) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Dialog(
        onDismissRequest = { /* Live call must be ended deliberately via End button or minimized */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(FridayDarkBg)
                .testTag("live_talk_overlay")
        ) {
            // Ambient Radial Glow Background
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
            ) {
                val center = Offset(size.width / 2f, size.height * 0.42f)
                val glowColor = when (liveTalkPhase) {
                    LiveTalkPhase.CONNECTING -> FridayCyan.copy(alpha = 0.25f)
                    LiveTalkPhase.LISTENING -> FridayGreen.copy(alpha = 0.32f)
                    LiveTalkPhase.THINKING -> FridayNeonBlue.copy(alpha = 0.35f)
                    LiveTalkPhase.SPEAKING -> FridayPurple.copy(alpha = 0.35f)
                    LiveTalkPhase.MUTED -> FridayRed.copy(alpha = 0.2f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FridayDarkSurfaceContainer.copy(alpha = 0.85f))
                        .border(1.dp, FridayBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing Live Indicator Dot
                        PulsingLiveDot(isActive = !isMuted)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FRIDAY CANLI SÖHBƏT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = FridayCyan
                                )
                            )
                            Text(
                                text = formattedDuration,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = FridayTextPrimary
                                )
                            )
                        }
                    }

                    // Quick Actions (Voice settings & Language)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FridayCyanDark.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, FridayCyan.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable {
                                    val nextLang = when (currentLanguage) {
                                        "AZ" -> "EN"
                                        "EN" -> "TR"
                                        "TR" -> "RU"
                                        else -> "AZ"
                                    }
                                    viewModel.setAssistantLanguage(nextLang)
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🌐 $currentLanguage",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showQuickVoiceSettings = !showQuickVoiceSettings },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Voice Tuning",
                                tint = FridayCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Holographic Voice Orb & Reactive Audio Visualizer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    LiveTalkOrb(
                        phase = liveTalkPhase,
                        rmsLevel = rmsLevel,
                        isMuted = isMuted,
                        onInterrupt = { viewModel.interruptLiveTalkSpeaking() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phase Status Badge
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = when (liveTalkPhase) {
                            LiveTalkPhase.CONNECTING -> FridayCyan.copy(alpha = 0.15f)
                            LiveTalkPhase.LISTENING -> FridayGreen.copy(alpha = 0.2f)
                            LiveTalkPhase.THINKING -> FridayNeonBlue.copy(alpha = 0.2f)
                            LiveTalkPhase.SPEAKING -> FridayPurple.copy(alpha = 0.2f)
                            LiveTalkPhase.MUTED -> FridayRed.copy(alpha = 0.2f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (liveTalkPhase) {
                                LiveTalkPhase.CONNECTING -> FridayCyan
                                LiveTalkPhase.LISTENING -> FridayGreen
                                LiveTalkPhase.THINKING -> FridayNeonBlue
                                LiveTalkPhase.SPEAKING -> FridayPurple
                                LiveTalkPhase.MUTED -> FridayRed
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (liveTalkPhase) {
                                    LiveTalkPhase.CONNECTING -> Icons.Default.Sync
                                    LiveTalkPhase.LISTENING -> Icons.Default.Mic
                                    LiveTalkPhase.THINKING -> Icons.Default.AutoAwesome
                                    LiveTalkPhase.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                                    LiveTalkPhase.MUTED -> Icons.Default.MicOff
                                },
                                contentDescription = null,
                                tint = when (liveTalkPhase) {
                                    LiveTalkPhase.CONNECTING -> FridayCyan
                                    LiveTalkPhase.LISTENING -> FridayGreen
                                    LiveTalkPhase.THINKING -> FridayNeonBlue
                                    LiveTalkPhase.SPEAKING -> FridayPurple
                                    LiveTalkPhase.MUTED -> FridayRed
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (liveTalkPhase) {
                                    LiveTalkPhase.CONNECTING -> "Əlaqə qurulur..."
                                    LiveTalkPhase.LISTENING -> "Sizi dinləyirəm, danışın..."
                                    LiveTalkPhase.THINKING -> "Friday düşünür..."
                                    LiveTalkPhase.SPEAKING -> "Friday cavab verir (Toxun: Dayandır)"
                                    LiveTalkPhase.MUTED -> "Mikrofon bağlıdır"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayTextPrimary
                                )
                            )
                        }
                    }
                }

                // Live Transcript & Subtitles Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = FridayDarkSurfaceContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, FridayBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // User's partial or latest prompt
                        if (partialText.isNotBlank() || latestUserPrompt.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(2.dp)),
                                    color = FridayCyanDark.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, FridayCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "SİZ",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = FridayCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (partialText.isNotBlank()) partialText else latestUserPrompt,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = FridayTextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Friday's live reply
                        if (latestFridayReply.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp).copy(bottomStart = androidx.compose.foundation.shape.CornerSize(2.dp)),
                                    color = FridayDarkSurfaceHigh,
                                    border = BorderStroke(1.dp, FridayBorder),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "FRIDAY AI",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = FridayPurple,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                            if (liveTalkPhase == LiveTalkPhase.SPEAKING) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Speaking",
                                                    tint = FridayPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = latestFridayReply,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = FridayTextPrimary,
                                                lineHeight = 20.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Quick suggestion pills inside Live Talk
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "💡 Təklif olunan suallar:",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayTextSecondary)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val livePrompts = listOf(
                                "Nə edə bilərsən?",
                                "Mənə hekayə danış",
                                "Telefon vəziyyəti",
                                "Fənəri yandır",
                                "Səsi 70% et",
                                "Günün xülasəsi"
                            )
                            items(livePrompts) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = FridayDarkSurface,
                                    border = BorderStroke(1.dp, FridayBorder),
                                    modifier = Modifier.clickable {
                                        viewModel.processSpokenCommand(prompt)
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelSmall.copy(color = FridayCyan),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Pitch/Speed Tuner Sheet if toggled
                AnimatedVisibility(
                    visible = showQuickVoiceSettings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    QuickVoiceTunerCard(
                        viewModel = viewModel,
                        onClose = { showQuickVoiceSettings = false }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Call Control Action Deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    IconButton(
                        onClick = { viewModel.toggleLiveTalkMute() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) FridayRed.copy(alpha = 0.25f) else FridayDarkSurfaceContainer)
                            .border(1.dp, if (isMuted) FridayRed else FridayBorder, CircleShape)
                            .testTag("live_talk_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) FridayRed else FridayCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // End Live Talk (Red Call End Button)
                    IconButton(
                        onClick = { viewModel.endLiveTalk() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(FridayRed)
                            .testTag("live_talk_end_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Live Talk",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Interrupt / Barge-in Speaking Button
                    IconButton(
                        onClick = { viewModel.interruptLiveTalkSpeaking() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (liveTalkPhase == LiveTalkPhase.SPEAKING) FridayPurple.copy(alpha = 0.25f) else FridayDarkSurfaceContainer)
                            .border(1.dp, if (liveTalkPhase == LiveTalkPhase.SPEAKING) FridayPurple else FridayBorder, CircleShape)
                            .testTag("live_talk_interrupt_button")
                    ) {
                        Icon(
                            imageVector = if (liveTalkPhase == LiveTalkPhase.SPEAKING) Icons.Default.StopCircle else Icons.Default.Hearing,
                            contentDescription = "Interrupt / Listen",
                            tint = if (liveTalkPhase == LiveTalkPhase.SPEAKING) FridayPurple else FridayCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingLiveDot(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (isActive) FridayGreen.copy(alpha = alpha) else FridayRed
            )
    )
}

@Composable
fun LiveTalkOrb(
    phase: LiveTalkPhase,
    rmsLevel: Float,
    isMuted: Boolean,
    onInterrupt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_live_anim")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    val orbPrimaryColor = when {
        isMuted -> FridayRed
        phase == LiveTalkPhase.LISTENING -> FridayGreen
        phase == LiveTalkPhase.THINKING -> FridayNeonBlue
        phase == LiveTalkPhase.SPEAKING -> FridayPurple
        else -> FridayCyan
    }

    val dynamicScale = when (phase) {
        LiveTalkPhase.LISTENING -> 1.0f + (rmsLevel * 0.45f)
        LiveTalkPhase.SPEAKING -> wavePulse * 1.05f
        LiveTalkPhase.THINKING -> wavePulse
        else -> 1.0f
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (phase == LiveTalkPhase.SPEAKING) onInterrupt()
            },
        contentAlignment = Alignment.Center
    ) {
        // Concentric Holographic HUD rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.88f

            // Outer Dashed HUD Ring
            drawCircle(
                color = orbPrimaryColor.copy(alpha = 0.35f),
                radius = baseRadius * dynamicScale,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )

            // Mid Orbiting Particles Ring
            for (i in 0 until 6) {
                val angle = Math.toRadians((rotationAngle + (i * 60.0)))
                val particleRadius = baseRadius * 0.72f * dynamicScale
                val px = center.x + (particleRadius * cos(angle)).toFloat()
                val py = center.y + (particleRadius * sin(angle)).toFloat()
                drawCircle(
                    color = orbPrimaryColor.copy(alpha = 0.7f),
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Inner Glowing Core
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(dynamicScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            orbPrimaryColor,
                            orbPrimaryColor.copy(alpha = 0.6f),
                            FridayDarkBg
                        )
                    )
                )
                .border(2.dp, orbPrimaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isMuted -> Icons.Default.MicOff
                    phase == LiveTalkPhase.LISTENING -> Icons.Default.GraphicEq
                    phase == LiveTalkPhase.THINKING -> Icons.Default.AutoAwesome
                    phase == LiveTalkPhase.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                    else -> Icons.Default.Hearing
                },
                contentDescription = "Orb State",
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

@Composable
fun QuickVoiceTunerCard(
    viewModel: FridayViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val malePitch by viewModel.malePitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FridayDarkSurfaceContainer,
        border = BorderStroke(1.dp, FridayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎙️ Kişi Səsi & Sürət Tənzimi",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayCyan
                    )
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = FridayTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pitch slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Səs Qalınlığı (Pitch):", style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary))
                Text(String.format("%.2fx", malePitch), style = MaterialTheme.typography.bodySmall.copy(color = FridayCyan, fontWeight = FontWeight.Bold))
            }
            Slider(
                value = malePitch,
                onValueChange = { viewModel.setMaleVoicePitch(it) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = FridayCyan,
                    activeTrackColor = FridayCyan,
                    inactiveTrackColor = FridayDarkSurfaceHigh
                )
            )

            // Speed slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Danışıq Sürəti:", style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary))
                Text(String.format("%.2fx", speechRate), style = MaterialTheme.typography.bodySmall.copy(color = FridayCyan, fontWeight = FontWeight.Bold))
            }
            Slider(
                value = speechRate,
                onValueChange = { viewModel.setSpeechRate(it) },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = FridayCyan,
                    activeTrackColor = FridayCyan,
                    inactiveTrackColor = FridayDarkSurfaceHigh
                )
            )
        }
    }
}
