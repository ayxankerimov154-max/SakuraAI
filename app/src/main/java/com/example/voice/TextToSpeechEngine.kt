package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onSpeechDoneListener: (() -> Unit)? = null

    // Male voice pitch & speech rate settings
    private val _malePitch = MutableStateFlow(0.82f)
    val malePitch: StateFlow<Float> = _malePitch.asStateFlow()

    private val _speechRate = MutableStateFlow(0.98f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    fun setOnSpeechDoneListener(listener: (() -> Unit)?) {
        this.onSpeechDoneListener = listener
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupMaleVoiceAndLanguage(Locale.getDefault())

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechDoneListener?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeechDoneListener?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onSpeechDoneListener?.invoke()
                }
            })
        } else {
            Log.e("TTS", "TextToSpeech initialization failed")
        }
    }

    /**
     * Sets language and dynamically finds/configures a masculine/male voice
     */
    fun setupMaleVoiceAndLanguage(targetLocale: Locale) {
        val engine = tts ?: return
        try {
            val result = engine.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallbacks if exact target isn't supported
                val azLocale = Locale.forLanguageTag("az-AZ")
                val trLocale = Locale.forLanguageTag("tr-TR")
                if (engine.setLanguage(azLocale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                    if (engine.setLanguage(trLocale) == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.language = Locale.US
                    }
                }
            }

            // Look for male voice in available voices
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val currentLang = engine.voice?.locale?.language ?: targetLocale.language
                val maleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    voice.locale.language.equals(currentLang, ignoreCase = true) &&
                            (name.contains("male") || name.contains("man") || name.contains("#male") || name.contains("masc"))
                } ?: voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("male") || name.contains("man") || name.contains("#male") || name.contains("masc"))
                }

                if (maleVoice != null) {
                    engine.voice = maleVoice
                }
            }

            // Set resonant male pitch and speech rate
            engine.setPitch(_malePitch.value)
            engine.setSpeechRate(_speechRate.value)
        } catch (e: Exception) {
            Log.w("TTS", "Male voice setup error: ${e.message}")
        }
    }

    /**
     * Detects approximate language of the text to select appropriate TTS locale and male voice
     */
    fun detectLanguageLocale(text: String): Locale {
        val trimmed = text.trim()
        val hasCyrillic = trimmed.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CYRILLIC }
        if (hasCyrillic) {
            return Locale.forLanguageTag("ru-RU")
        }

        val hasArabic = trimmed.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
        if (hasArabic) {
            return Locale.forLanguageTag("ar-SA")
        }

        val hasCJK = trimmed.any {
            Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            Character.UnicodeBlock.of(it) == Character.UnicodeBlock.HIRAGANA ||
            Character.UnicodeBlock.of(it) == Character.UnicodeBlock.KATAKANA ||
            Character.UnicodeBlock.of(it) == Character.UnicodeBlock.HANGUL_SYLLABLES
        }
        if (hasCJK) {
            return Locale.CHINESE
        }

        // Check for specific Azerbaijani / Turkish characters
        val azTrChars = setOf('ə', 'Ə', 'ı', 'I', 'ğ', 'Ğ', 'ş', 'Ş', 'ç', 'Ç', 'ö', 'Ö', 'ü', 'Ü')
        val hasAzTrChar = trimmed.any { it in azTrChars }
        if (hasAzTrChar) {
            return Locale.forLanguageTag("az-AZ")
        }

        // Simple word heuristics for common languages
        val words = trimmed.lowercase().split(Regex("\\s+"))
        val germanWords = setOf("der", "die", "das", "und", "ist", "nicht", "ich", "hallo", "guten", "tag", "wie", "geht")
        val frenchWords = setOf("bonjour", "le", "la", "les", "et", "est", "je", "vous", "merci", "oui")
        val spanishWords = setOf("hola", "el", "la", "y", "es", "gracias", "por", "favor", "como", "estas")
        val italianWords = setOf("ciao", "grazie", "buongiorno", "come", "stai", "per", "favore")
        val englishWords = setOf("the", "is", "are", "you", "and", "hello", "hi", "hey", "what", "how", "can", "help", "turn", "wifi", "battery")

        if (words.any { it in germanWords }) return Locale.GERMANY
        if (words.any { it in frenchWords }) return Locale.FRANCE
        if (words.any { it in spanishWords }) return Locale.forLanguageTag("es-ES")
        if (words.any { it in italianWords }) return Locale.ITALY
        if (words.any { it in englishWords }) return Locale.US

        return Locale.forLanguageTag("az-AZ")
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isInitialized || tts == null || text.isBlank()) return

        val detectedLocale = detectLanguageLocale(text)
        setupMaleVoiceAndLanguage(detectedLocale)

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "friday_utt_${System.currentTimeMillis()}")
        }
        tts?.speak(text, queueMode, params, "friday_utt_${System.currentTimeMillis()}")
    }

    fun setMaleVoicePitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.5f)
        _malePitch.value = clamped
        tts?.setPitch(clamped)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun resetVoiceDefaults() {
        setMaleVoicePitch(0.82f)
        setSpeechRate(0.98f)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
