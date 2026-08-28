package com.example.data.repository

import android.content.Context
import com.example.data.db.FileDocumentDao
import com.example.data.model.FileDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository(
    private val fileDocumentDao: FileDocumentDao,
    private val context: Context
) {
    val allFiles: Flow<List<FileDocumentEntity>> = fileDocumentDao.getAllFiles()

    private val documentsDir: File
        get() {
            val dir = File(context.filesDir, "friday_documents")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getDocumentsDirectory(): File = documentsDir

    suspend fun createFile(
        name: String,
        content: String,
        extension: String = "txt"
    ): FileDocumentEntity = withContext(Dispatchers.IO) {
        val cleanName = if (name.endsWith(".$extension")) name else "$name.$extension"
        val targetFile = File(documentsDir, cleanName)
        targetFile.writeText(content)

        val entity = FileDocumentEntity(
            fileName = cleanName,
            fileExtension = extension,
            filePath = targetFile.absolutePath,
            content = content,
            sizeBytes = targetFile.length(),
            updatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        val id = fileDocumentDao.insertFile(entity)
        entity.copy(id = id)
    }

    suspend fun updateFile(
        id: Long,
        newContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = fileDocumentDao.getFileById(id) ?: return@withContext false
        val file = File(existing.filePath)
        file.writeText(newContent)

        val updated = existing.copy(
            content = newContent,
            sizeBytes = file.length(),
            updatedAt = System.currentTimeMillis()
        )
        fileDocumentDao.updateFile(updated)
        true
    }

    suspend fun deleteFile(entity: FileDocumentEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        fileDocumentDao.deleteFile(entity)
    }

    suspend fun deleteByName(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val existing = fileDocumentDao.getFileByName(fileName)
        if (existing != null) {
            deleteFile(existing)
            true
        } else {
            val targetFile = File(documentsDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
                true
            } else false
        }
    }
}
