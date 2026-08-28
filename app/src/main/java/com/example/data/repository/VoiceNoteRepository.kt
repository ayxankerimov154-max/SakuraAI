package com.example.data.repository

import android.content.Context
import com.example.data.db.VoiceNoteDao
import com.example.data.model.VoiceNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VoiceNoteRepository(
    private val voiceNoteDao: VoiceNoteDao,
    private val context: Context
) {
    val allNotes: Flow<List<VoiceNoteEntity>> = voiceNoteDao.getAllVoiceNotes()

    private val audioDir: File
        get() {
            val dir = File(context.filesDir, "voice_notes")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getAudioDirectory(): File = audioDir

    suspend fun saveVoiceNote(
        title: String,
        file: File,
        durationMs: Long,
        transcript: String = ""
    ): VoiceNoteEntity = withContext(Dispatchers.IO) {
        val note = VoiceNoteEntity(
            title = title.ifBlank { "Səsli Qeyd ${System.currentTimeMillis() % 10000}" },
            fileName = file.name,
            filePath = file.absolutePath,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis(),
            transcript = transcript,
            sizeBytes = file.length()
        )
        val id = voiceNoteDao.insertVoiceNote(note)
        note.copy(id = id)
    }

    suspend fun deleteVoiceNote(note: VoiceNoteEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(note.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        voiceNoteDao.deleteVoiceNote(note)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val note = voiceNoteDao.getNoteById(id)
        if (note != null) {
            deleteVoiceNote(note)
        }
    }
}
