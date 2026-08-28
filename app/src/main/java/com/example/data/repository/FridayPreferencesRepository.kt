package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FridayPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("friday_ai_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_LANGUAGE = "assistant_language"
        private const val KEY_HOTWORD_ENABLED = "hotword_enabled"
        private const val KEY_BACKGROUND_LISTENING = "background_listening_enabled"
        private const val KEY_SAVE_HISTORY = "save_history_enabled"
    }

    private val _geminiApiKey = MutableStateFlow(
        prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    )
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(
        prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    )
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _voicePitch = MutableStateFlow(
        prefs.getFloat(KEY_VOICE_PITCH, 0.82f)
    )
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _speechRate = MutableStateFlow(
        prefs.getFloat(KEY_SPEECH_RATE, 0.98f)
    )
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _language = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, "AZ") ?: "AZ"
    )
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isHotwordEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_HOTWORD_ENABLED, true)
    )
    val isHotwordEnabled: StateFlow<Boolean> = _isHotwordEnabled.asStateFlow()

    private val _isBackgroundListeningEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BACKGROUND_LISTENING, false)
    )
    val isBackgroundListeningEnabled: StateFlow<Boolean> = _isBackgroundListeningEnabled.asStateFlow()

    private val _isSaveHistoryEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SAVE_HISTORY, true)
    )
    val isSaveHistoryEnabled: StateFlow<Boolean> = _isSaveHistoryEnabled.asStateFlow()

    fun getEffectiveApiKey(): String {
        val customKey = _geminiApiKey.value.trim()
        if (customKey.isNotBlank()) {
            return customKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "YOUR_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotBlank() && key.length >= 25
    }

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        _geminiApiKey.value = trimmed
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        if (trimmed.isNotBlank()) {
            setFirstLaunchCompleted()
        }
    }

    fun setFirstLaunchCompleted() {
        _isFirstLaunch.value = false
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply()
    }

    fun setVoicePitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.5f)
        _voicePitch.value = clamped
        prefs.edit().putFloat(KEY_VOICE_PITCH, clamped).apply()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clamped
        prefs.edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun setHotwordEnabled(enabled: Boolean) {
        _isHotwordEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HOTWORD_ENABLED, enabled).apply()
    }

    fun setBackgroundListeningEnabled(enabled: Boolean) {
        _isBackgroundListeningEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BACKGROUND_LISTENING, enabled).apply()
    }

    fun setSaveHistoryEnabled(enabled: Boolean) {
        _isSaveHistoryEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
    }

    fun resetVoiceDefaults() {
        setVoicePitch(0.82f)
        setSpeechRate(0.98f)
    }
}
