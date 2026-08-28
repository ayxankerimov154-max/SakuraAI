package com.example.data.repository

import com.example.data.db.ChatMessageDao
import com.example.data.model.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()
    val messageCount: Flow<Int> = chatMessageDao.getMessageCount()

    suspend fun saveMessage(
        sender: String,
        text: String,
        actionType: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val entity = ChatMessageEntity(
            sender = sender,
            text = text,
            timestamp = timestamp,
            actionType = actionType
        )
        return chatMessageDao.insertMessage(entity)
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }

    suspend fun deleteMessage(id: Long) {
        chatMessageDao.deleteMessage(id)
    }
}
