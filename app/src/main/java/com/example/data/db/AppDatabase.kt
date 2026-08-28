package com.example.data.db

import androidx.room.*
import com.example.data.model.AssistantLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FileDocumentEntity
import com.example.data.model.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): VoiceNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(note: VoiceNoteEntity): Long

    @Update
    suspend fun updateVoiceNote(note: VoiceNoteEntity)

    @Delete
    suspend fun deleteVoiceNote(note: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FileDocumentDao {
    @Query("SELECT * FROM file_documents ORDER BY updatedAt DESC")
    fun getAllFiles(): Flow<List<FileDocumentEntity>>

    @Query("SELECT * FROM file_documents WHERE id = :id")
    suspend fun getFileById(id: Long): FileDocumentEntity?

    @Query("SELECT * FROM file_documents WHERE fileName = :fileName LIMIT 1")
    suspend fun getFileByName(fileName: String): FileDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileDocumentEntity): Long

    @Update
    suspend fun updateFile(file: FileDocumentEntity)

    @Delete
    suspend fun deleteFile(file: FileDocumentEntity)

    @Query("DELETE FROM file_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM file_documents WHERE fileName = :fileName")
    suspend fun deleteByName(fileName: String)
}

@Dao
interface AssistantLogDao {
    @Query("SELECT * FROM assistant_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AssistantLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AssistantLogEntity): Long

    @Query("DELETE FROM assistant_logs")
    suspend fun clearLogs()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages")
    fun getMessageCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Database(
    entities = [
        VoiceNoteEntity::class,
        FileDocumentEntity::class,
        AssistantLogEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun fileDocumentDao(): FileDocumentDao
    abstract fun assistantLogDao(): AssistantLogDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "friday_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
