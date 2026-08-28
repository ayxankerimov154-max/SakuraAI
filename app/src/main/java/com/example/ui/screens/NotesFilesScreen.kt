package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.FileDocumentEntity
import com.example.data.model.VoiceNoteEntity
import com.example.ui.FridayViewModel
import com.example.ui.components.AudioWaveform
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesFilesScreen(
    viewModel: FridayViewModel,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Voice Notes, 1 = File Documents
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var recordingTitleInput by remember { mutableStateOf("") }
    var showSaveNoteDialog by remember { mutableStateOf(false) }

    val voiceNotes by viewModel.allVoiceNotes.collectAsState()
    val files by viewModel.allFiles.collectAsState()
    val isRecording by viewModel.isRecordingAudio.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FridayDarkBg)
            .padding(16.dp)
    ) {
        // Sub-Tabs Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FridayDarkSurfaceContainer)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (subTab == 0) FridayCyan else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_voice_notes"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (subTab == 0) Color.Black else FridayTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Səsli Qeydlər (${voiceNotes.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == 0) Color.Black else FridayTextSecondary
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (subTab == 1) FridayCyan else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 10.dp)
                    .testTag("tab_files"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (subTab == 1) Color.Black else FridayTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fayl Meneceri (${files.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == 1) Color.Black else FridayTextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subTab == 0) {
            // --- VOICE NOTES TAB ---
            // Audio Recorder Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FridayDarkSurfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRecording) FridayRed else FridayBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRecording) "Səsli Qeyd Yazılır..." else "Yeni Səsli Qeyd Apar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) FridayRed else FridayTextPrimary
                            )
                        )

                        if (isRecording) {
                            val seconds = (recordingDurationMs / 1000) % 60
                            val minutes = (recordingDurationMs / (1000 * 60)) % 60
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FridayRed
                                )
                            )
                        }
                    }

                    if (isRecording) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AudioWaveform(
                            isActive = true,
                            amplitude = 0.8f,
                            color = FridayRed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            OutlinedButton(
                                onClick = { viewModel.cancelVoiceRecording() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FridayTextSecondary)
                            ) {
                                Text("Ləğv et")
                            }

                            Button(
                                onClick = {
                                    showSaveNoteDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FridayRed),
                                modifier = Modifier.testTag("stop_recording_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dayandır & Saxla")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startVoiceRecording() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                                modifier = Modifier.testTag("start_recording_button")
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Səsli Qeydə Başla",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Notes List
            if (voiceNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MicNone,
                            contentDescription = null,
                            tint = FridayTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hələ ki səsli qeyd yoxdur",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FridayTextSecondary)
                        )
                        Text(
                            text = "\"Səsli qeyd yaz\" əmrini verə və ya yuxarıdakı düyməyə toxuna bilərsiniz",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(voiceNotes, key = { it.id }) { note ->
                        VoiceNoteItemCard(
                            note = note,
                            isPlaying = playbackState.isPlaying && playbackState.activeNoteId == note.id,
                            currentPosMs = if (playbackState.activeNoteId == note.id) playbackState.currentPositionMs else 0,
                            totalDurationMs = if (playbackState.activeNoteId == note.id) playbackState.totalDurationMs else note.durationMs.toInt(),
                            onPlayToggle = {
                                if (playbackState.isPlaying && playbackState.activeNoteId == note.id) {
                                    viewModel.pauseVoiceNote()
                                } else {
                                    viewModel.playVoiceNote(note)
                                }
                            },
                            onSeek = { viewModel.seekVoiceNote(it) },
                            onDelete = { viewModel.deleteVoiceNote(note) }
                        )
                    }
                }
            }
        } else {
            // --- FILE DOCUMENTS TAB ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yaradılmış Fayllar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayTextPrimary
                    )
                )

                Button(
                    onClick = { showCreateFileDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                    modifier = Modifier.testTag("create_file_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Fayl", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = FridayTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hələ ki heç bir sənəd faylı yaradılmayıb",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FridayTextSecondary)
                        )
                        Text(
                            text = "\"Fayl yarat: qeyd1 məzmun: ...\" deyərək səslə fayl yaradın",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(files, key = { it.id }) { file ->
                        FileDocumentItemCard(
                            file = file,
                            onEdit = { viewModel.openFileEditor(file) },
                            onDelete = { viewModel.deleteFile(file) }
                        )
                    }
                }
            }
        }
    }

    // Save Note Title Dialog
    if (showSaveNoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveNoteDialog = false
                viewModel.stopVoiceRecording(recordingTitleInput)
                recordingTitleInput = ""
            },
            title = { Text("Səsli Qeydi Saxla", color = FridayTextPrimary) },
            text = {
                OutlinedTextField(
                    value = recordingTitleInput,
                    onValueChange = { recordingTitleInput = it },
                    label = { Text("Qeydin Başlığı") },
                    placeholder = { Text("məs: Görüş Qeydləri") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveNoteDialog = false
                        viewModel.stopVoiceRecording(recordingTitleInput)
                        recordingTitleInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FridayCyan)
                ) {
                    Text("Saxla", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveNoteDialog = false
                    viewModel.stopVoiceRecording("")
                    recordingTitleInput = ""
                }) {
                    Text("Susmaya görə saxla")
                }
            }
        )
    }

    // Create New File Dialog
    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onCreate = { name, content, ext ->
                viewModel.createFile(name, content, ext)
                showCreateFileDialog = false
            }
        )
    }
}

@Composable
fun VoiceNoteItemCard(
    note: VoiceNoteEntity,
    isPlaying: Boolean,
    currentPosMs: Int,
    totalDurationMs: Int,
    onPlayToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(note.createdAt) { dateFormat.format(Date(note.createdAt)) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlaying) FridayCyan.copy(alpha = 0.6f) else FridayBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isPlaying) FridayCyan else FridayDarkSurfaceHigh, CircleShape)
                            .testTag("play_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pauza" else "Dinlə",
                            tint = if (isPlaying) Color.Black else FridayCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "${note.fileName} • ${note.sizeBytes / 1024} KB • $formattedDate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Sil",
                        tint = FridayRed.copy(alpha = 0.8f)
                    )
                }
            }

            if (isPlaying || currentPosMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = currentPosMs.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..totalDurationMs.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = FridayCyan,
                        activeTrackColor = FridayCyan,
                        inactiveTrackColor = FridayDarkSurfaceHigh
                    )
                )
            }
        }
    }
}

@Composable
fun FileDocumentItemCard(
    file: FileDocumentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(file.updatedAt) { dateFormat.format(Date(file.updatedAt)) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FridayDarkSurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, FridayBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FridayPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".${file.fileExtension}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayPurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FridayTextPrimary
                            )
                        )
                        Text(
                            text = "${file.sizeBytes} bayt • $formattedDate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FridayTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_file_${file.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzəliş et",
                            tint = FridayCyan
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_file_${file.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Sil",
                            tint = FridayRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = FridayDarkBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = file.content.ifBlank { "(Boş fayl)" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = FridayTextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, content: String, ext: String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var selectedExt by remember { mutableStateOf("txt") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Fayl Yarat", style = MaterialTheme.typography.titleLarge.copy(color = FridayTextPrimary))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Faylın adı") },
                    placeholder = { Text("məs: layihə_qeydləri") },
                    modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("txt", "md", "json").forEach { ext ->
                        FilterChip(
                            selected = selectedExt == ext,
                            onClick = { selectedExt = ext },
                            label = { Text(".$ext") }
                        )
                    }
                }

                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { fileContent = it },
                    label = { Text("Faylın Məzmunu") },
                    placeholder = { Text("Mətni bura yazın...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("new_file_content_input"),
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onCreate(fileName.trim(), fileContent, selectedExt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                modifier = Modifier.testTag("confirm_create_file_btn")
            ) {
                Text("Yarat", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et")
            }
        }
    )
}
