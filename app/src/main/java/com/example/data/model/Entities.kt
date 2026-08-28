package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val transcript: String = "",
    val sizeBytes: Long = 0L
)

@Entity(tableName = "file_documents")
data class FileDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileExtension: String,
    val filePath: String,
    val content: String,
    val sizeBytes: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assistant_logs")
data class AssistantLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "COMMAND", "CALL", "SMS", "CHAT", "SYSTEM"
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
