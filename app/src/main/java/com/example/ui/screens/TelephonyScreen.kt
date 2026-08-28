package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telephony.CommsEvent
import com.example.ui.FridayViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TelephonyScreen(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    var callNumberInput by remember { mutableStateOf("+994501234567") }
    var smsNumberInput by remember { mutableStateOf("+994501234567") }
    var smsMessageInput by remember { mutableStateOf("Salam, Friday AI vasitəsilə göndərildi.") }

    val incomingCall by viewModel.incomingCallAlert.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
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
        Column {
            Text(
                text = "Zəng & SMS İdarəetmə Mərkəzi",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = FridayTextPrimary
                )
            )
            Text(
                text = "Səsli əmrlərlə zəng edin, SMS göndərin, gələn zəngləri qəbul və ya rədd edin",
                style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
            )
        }

        // Active Incoming Call Banner (If Ringing)
        AnimatedVisibility(visible = incomingCall != null) {
            val call = incomingCall
            if (call != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FridayDarkSurfaceHigh,
                    border = androidx.compose.foundation.BorderStroke(2.dp, FridayGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incoming_call_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(FridayGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GƏLƏN ZƏNG",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayGreen,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = call.callerName ?: call.callerNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        if (call.callerName != null) {
                            Text(
                                text = call.callerNumber,
                                style = MaterialTheme.typography.bodyMedium.copy(color = FridayTextSecondary)
                            )
                        }

                        Text(
                            text = "Səsli əmr: \"Zəngi qəbul et\" və ya \"Zəngi rədd et\"",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FridayCyan,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Reject Button
                            Button(
                                onClick = { viewModel.rejectIncomingCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = FridayRed),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .testTag("reject_call_btn")
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rədd et")
                            }

                            // Answer Button
                            Button(
                                onClick = { viewModel.answerIncomingCall() },
                                colors = ButtonDefaults.buttonColors(containerColor = FridayGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                                    .testTag("answer_call_btn")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Qəbul et", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Call Dial Card ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = FridayDarkSurfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FridayGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = FridayGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Səsli Zəng Paneli",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Səsli əmr: \"+994501234567 nömrəsinə zəng et\"",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayGreen)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = callNumberInput,
                        onValueChange = { callNumberInput = it },
                        label = { Text("Telefon Nömrəsi") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (callNumberInput.isNotBlank()) {
                                viewModel.makeCall(callNumberInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FridayGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("dial_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zəng et", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SMS Composer Card ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = FridayDarkSurfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FridayNeonBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = FridayNeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMS Göndərmə Paneli",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Səsli əmr: \"SMS göndər [Nömrə]: [Mətn]\"",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = smsNumberInput,
                    onValueChange = { smsNumberInput = it },
                    label = { Text("Kimə (Nömrə)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_number_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = smsMessageInput,
                    onValueChange = { smsMessageInput = it },
                    label = { Text("SMS Mətni") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_message_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (smsNumberInput.isNotBlank() && smsMessageInput.isNotBlank()) {
                            viewModel.sendSms(smsNumberInput, smsMessageInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FridayNeonBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_sms_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS Göndər", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Incoming Announcements Simulator Card ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = FridayDarkSurfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, FridayPurple.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FridayPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = FridayPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Səsli Elan & Zəng Test Simulyatoru",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "Gələn zəng və SMS-in səsli oxunmasını birbaşa test edin",
                            style = MaterialTheme.typography.labelSmall.copy(color = FridayPurple)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Simulate Incoming Call Button
                    OutlinedButton(
                        onClick = {
                            viewModel.simulateIncomingCall(
                                callerName = "Əhməd Qasımov",
                                callerNumber = "+994501234567"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_call_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayGreen)
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zəng Elanı Test", fontSize = 11.sp)
                    }

                    // Simulate Incoming SMS Button
                    OutlinedButton(
                        onClick = {
                            viewModel.simulateIncomingSms(
                                senderName = "Leyla Məmmədova",
                                senderNumber = "+994559876543",
                                message = "Salam, axşam saat 8-də görüşümüz qüvvədədir?"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_sms_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayCyan)
                    ) {
                        Icon(Icons.Default.MarkChatUnread, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS Elanı Test", fontSize = 11.sp)
                    }
                }
            }
        }

        // --- Activity Logs ---
        Text(
            text = "Son Sistem & Əlaqə Fəaliyyətləri",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = FridayTextPrimary
            )
        )

        if (recentLogs.isEmpty()) {
            Text(
                text = "Hələ ki fəaliyyət qeydiyyatı yoxdur.",
                style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary)
            )
        } else {
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            recentLogs.take(5).forEach { log ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FridayDarkSurfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${log.type}] ${log.prompt}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayCyan
                                )
                            )
                            Text(
                                text = log.response,
                                style = MaterialTheme.typography.bodySmall.copy(color = FridayTextSecondary),
                                maxLines = 1
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
