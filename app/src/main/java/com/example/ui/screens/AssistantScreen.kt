package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.ChatMessage
import com.example.ui.FridayViewModel
import com.example.ui.components.AudioWaveform
import com.example.ui.components.VoiceOrb
import com.example.ui.theme.*
import com.example.voice.SpeechState
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    val speechState by viewModel.speechState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isHotwordEnabled by viewModel.isHotwordEnabled.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isListening = speechState is SpeechState.Listening || speechState is SpeechState.HotwordListening

    val samplePrompts = listOf(
        "Wi-Fi aç",
        "Bluetooth qoş",
        "Səsi 80% et",
        "Parlaqlığı artır",
        "Fənəri yandır",
        "Səsli qeyd yaz",
        "Fayl yarat: qeyd1 məzmun: Test",
        "+994501234567 zəng et",
        "Batareya nə qədərdir?"
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FridayDarkBg)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Status Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FridayDarkSurfaceContainer)
                .border(1.dp, FridayBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSpeaking) FridayPurple
                            else if (isListening) FridayGreen
                            else FridayCyan
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSpeaking -> "Friday danışır..."
                        speechState is SpeechState.Listening -> "Sizi dinləyirəm..."
                        speechState is SpeechState.HotwordListening -> "\"Hey Friday\" gözlənilir"
                        else -> "FRIDAY Onlayn"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = FridayTextPrimary
                    )
                )
            }

            // Hotword switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.toggleHotword(!isHotwordEnabled) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Hey Friday",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isHotwordEnabled) FridayCyan else FridayTextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isHotwordEnabled,
                    onCheckedChange = { viewModel.toggleHotword(it) },
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("hotword_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FridayCyan,
                        checkedTrackColor = FridayCyanDark.copy(alpha = 0.4f),
                        uncheckedThumbColor = FridayTextSecondary,
                        uncheckedTrackColor = FridayDarkSurfaceHigh
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Voice Orb
        VoiceOrb(
            isListening = isListening,
            isSpeaking = isSpeaking,
            rmsLevel = rmsLevel,
            onClick = {
                if (isListening) viewModel.stopListening()
                else viewModel.startListening()
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Audio Waveform
        AudioWaveform(
            isActive = isListening || isSpeaking,
            amplitude = if (isListening) rmsLevel else 0.6f,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samplePrompts) { prompt ->
                SuggestionChip(
                    onClick = { viewModel.processSpokenCommand(prompt) },
                    label = {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium.copy(color = FridayCyan)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = FridayDarkSurfaceContainer
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = FridayBorder
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("chip_$prompt")
                )
            }
        }

        // Conversation History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onSpeak = { viewModel.speakText(msg.text) }
                )
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = FridayDarkSurfaceContainer,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isListening) viewModel.stopListening()
                        else viewModel.startListening()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isListening) FridayGreen.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                        .testTag("mic_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) FridayGreen else FridayCyan
                    )
                }

                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = "Friday-ə əmr verin və ya sual soruşun...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FridayTextSecondary)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_text_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = FridayTextPrimary,
                        unfocusedTextColor = FridayTextPrimary
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendTextMessage(textInput.trim())
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = FridayCyan
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == "USER"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FridayCyanDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) FridayNeonBlue.copy(alpha = 0.35f) else FridayDarkSurfaceHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) FridayNeonBlue.copy(alpha = 0.5f) else FridayBorder
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.actionType != null && !isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FridayGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.actionType,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FridayGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FridayTextPrimary,
                        lineHeight = 20.sp
                    )
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Text",
                            tint = FridayCyan.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSpeak() }
                        )
                    }
                }
            }
        }
    }
}
