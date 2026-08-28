package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FridayViewModel
import com.example.ui.components.ApiKeyOnboardingDialog
import com.example.ui.components.VoiceSettingsOverlay
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.FileEditorDialog
import com.example.ui.screens.FridaySettingsScreen
import com.example.ui.screens.NotesFilesScreen
import com.example.ui.screens.TelephonyScreen
import com.example.ui.screens.WidgetsScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FridayApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridayApp(viewModel: FridayViewModel = viewModel()) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val editingFile by viewModel.editingFile.collectAsState()
    val isAwakeModalOpen by viewModel.isHotwordAwakeModalOpen.collectAsState()
    val isVoiceSettingsOverlayOpen by viewModel.isVoiceSettingsOverlayOpen.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    var showFirstLaunchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isFirstLaunch, geminiApiKey) {
        if (isFirstLaunch && geminiApiKey.isBlank()) {
            showFirstLaunchDialog = true
        }
    }

    // Permission Launcher
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle granted permissions
    }

    LaunchedEffect(Unit) {
        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FridayDarkBg),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.friday_ai_logo_1787859726049),
                            contentDescription = "Friday Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FRIDAY AI",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = FridayCyan,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeaking) FridayPurple else FridayGreen)
                                )
                            }
                            Text(
                                text = "Sistem & Səs Köməkçisi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FridayTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openVoiceSettingsOverlay() },
                        modifier = Modifier.testTag("appbar_voice_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice & Wake Word Settings",
                            tint = FridayCyan
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isSpeaking) viewModel.stopSpeaking()
                            else viewModel.speakText("Salam! Mən Friday, sizin şəxsi AI köməkçinizəm.")
                        },
                        modifier = Modifier.testTag("appbar_tts_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "TTS Status",
                            tint = if (isSpeaking) FridayPurple else FridayCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FridayDarkBg,
                    titleContentColor = FridayTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = FridayDarkSurface,
                contentColor = FridayTextPrimary,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                // Tab 0: AI Səs & Əmrlər
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Psychology else Icons.Default.PsychologyAlt,
                            contentDescription = "AI Köməkçi"
                        )
                    },
                    label = { Text("Friday AI", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = FridayCyan,
                        indicatorColor = FridayCyan,
                        unselectedIconColor = FridayTextSecondary,
                        unselectedTextColor = FridayTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_assistant")
                )

                // Tab 1: Telefon Ayarları Widgets
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.Widgets else Icons.Default.Tune,
                            contentDescription = "Widget-lər"
                        )
                    },
                    label = { Text("Sistem", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = FridayCyan,
                        indicatorColor = FridayCyan,
                        unselectedIconColor = FridayTextSecondary,
                        unselectedTextColor = FridayTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_widgets")
                )

                // Tab 2: Səsli Qeydlər & Fayllar
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.FolderSpecial else Icons.Default.Folder,
                            contentDescription = "Qeydlər & Fayllar"
                        )
                    },
                    label = { Text("Qeyd & Fayl", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = FridayCyan,
                        indicatorColor = FridayCyan,
                        unselectedIconColor = FridayTextSecondary,
                        unselectedTextColor = FridayTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_notes")
                )

                // Tab 3: Zəng & SMS İdarəsi
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Default.ContactPhone else Icons.Default.Phone,
                            contentDescription = "Zəng & SMS"
                        )
                    },
                    label = { Text("Zəng & SMS", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = FridayCyan,
                        indicatorColor = FridayCyan,
                        unselectedIconColor = FridayTextSecondary,
                        unselectedTextColor = FridayTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_comms")
                )

                // Tab 4: Friday Ayarları (Dedicated Settings)
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.SettingsSuggest else Icons.Default.Tune,
                            contentDescription = "Friday Ayarları"
                        )
                    },
                    label = { Text("Ayarlar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = FridayCyan,
                        indicatorColor = FridayCyan,
                        unselectedIconColor = FridayTextSecondary,
                        unselectedTextColor = FridayTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FridayDarkBg)
        ) {
            when (selectedTab) {
                0 -> AssistantScreen(viewModel = viewModel)
                1 -> WidgetsScreen(viewModel = viewModel)
                2 -> NotesFilesScreen(viewModel = viewModel)
                3 -> TelephonyScreen(viewModel = viewModel)
                4 -> FridaySettingsScreen(viewModel = viewModel)
            }

            // First Launch Gemini API Key Setup Dialog
            if (showFirstLaunchDialog) {
                ApiKeyOnboardingDialog(
                    initialKey = geminiApiKey,
                    isDismissible = true,
                    onSaveKey = { key ->
                        viewModel.saveGeminiApiKey(key)
                        viewModel.dismissFirstLaunchModal()
                        showFirstLaunchDialog = false
                    },
                    onDismiss = {
                        viewModel.dismissFirstLaunchModal()
                        showFirstLaunchDialog = false
                    }
                )
            }

            // Voice & 'Hey Friday' Settings Overlay Menu
            if (isVoiceSettingsOverlayOpen) {
                VoiceSettingsOverlay(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeVoiceSettingsOverlay() }
                )
            }

            // Built-in File Editor Modal
            editingFile?.let { file ->
                FileEditorDialog(
                    file = file,
                    onDismiss = { viewModel.closeFileEditor() },
                    onSave = { newContent ->
                        viewModel.saveFileEdit(newContent)
                    }
                )
            }

            // "Hey Friday" Hotword Awakening Alert
            if (isAwakeModalOpen) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissAwakeModal() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = FridayCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hey Friday!", color = FridayCyan, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            text = "Bəli, sizi dinləyirəm! Əmrinizi və ya sualınızı səsləndirin...",
                            color = FridayTextPrimary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissAwakeModal()
                                viewModel.startListening()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FridayCyan)
                        ) {
                            Text("Danış", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissAwakeModal() }) {
                            Text("Bağla")
                        }
                    }
                )
            }
        }
    }
}
