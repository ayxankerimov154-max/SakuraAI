package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.FridayPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Friday AI", appName)
    }

    @Test
    fun `test preferences repository persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FridayPreferencesRepository(context)

        repo.setGeminiApiKey("AIzaSyTestKey123")
        assertEquals("AIzaSyTestKey123", repo.geminiApiKey.value)

        repo.setVoicePitch(0.85f)
        assertEquals(0.85f, repo.voicePitch.value, 0.001f)

        repo.setSpeechRate(1.15f)
        assertEquals(1.15f, repo.speechRate.value, 0.001f)

        repo.setLanguage("AZ")
        assertEquals("AZ", repo.language.value)

        repo.setHotwordEnabled(true)
        assertTrue(repo.isHotwordEnabled.value)
    }

    @Test
    fun `test chat persistence in database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val chatRepo = ChatRepository(db.chatMessageDao())

        chatRepo.clearHistory()
        chatRepo.saveMessage("USER", "Salam Friday", null)
        chatRepo.saveMessage("FRIDAY", "Salam! Sizə necə kömək edə bilərəm?", "AI")

        val messages = chatRepo.allMessages.first()
        assertEquals(2, messages.size)
        assertEquals("USER", messages[0].sender)
        assertEquals("Salam Friday", messages[0].text)
        assertEquals("FRIDAY", messages[1].sender)
    }
}
