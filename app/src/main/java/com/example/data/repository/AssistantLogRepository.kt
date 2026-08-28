package com.example.data.repository

import com.example.data.db.AssistantLogDao
import com.example.data.model.AssistantLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AssistantLogRepository(
    private val assistantLogDao: AssistantLogDao
) {
    val recentLogs: Flow<List<AssistantLogEntity>> = assistantLogDao.getRecentLogs()

    suspend fun logAction(
        type: String,
        prompt: String,
        response: String,
        isSuccess: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val entity = AssistantLogEntity(
            type = type,
            prompt = prompt,
            response = response,
            timestamp = System.currentTimeMillis(),
            isSuccess = isSuccess
        )
        assistantLogDao.insertLog(entity)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        assistantLogDao.clearLogs()
    }
}
