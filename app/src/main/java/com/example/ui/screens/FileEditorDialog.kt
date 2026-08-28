package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileDocumentEntity
import com.example.ui.theme.FridayCyan
import com.example.ui.theme.FridayTextPrimary

@Composable
fun FileEditorDialog(
    file: FileDocumentEntity,
    onDismiss: () -> Unit,
    onSave: (newContent: String) -> Unit
) {
    var contentText by remember { mutableStateOf(file.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FridayTextPrimary
                    )
                )
                Text(
                    text = "Məzmunu düzəliş edin və yaddaşda saxlayın",
                    style = MaterialTheme.typography.bodySmall.copy(color = FridayCyan, fontSize = 11.sp)
                )
            }
        },
        text = {
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("Fayl Məzmunu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("file_editor_textarea"),
                maxLines = 15,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(contentText) },
                colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                modifier = Modifier.testTag("save_file_edit_btn")
            ) {
                Text("Saxla", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et")
            }
        }
    )
}
