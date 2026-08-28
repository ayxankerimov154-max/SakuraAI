package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    object HotwordListening : SpeechState()
    data class Recognized(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class VoiceSpeechManager(
    private val context: Context,
    private val onHotwordTriggered: () -> Unit,
    private val onCommandRecognized: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _isHotwordEnabled = MutableStateFlow(true)
    val isHotwordEnabled: StateFlow<Boolean> = _isHotwordEnabled.asStateFlow()

    private var isExplicitListening = false
    private var isContinuousListening = false

    private val hotwords = listOf(
        "hey fida", "fida", "ay fida", "ey fida", "hey fiday", "fiday",
        "hey friday", "friday", "ey friday", "hey fraydey", "fraydey",
        "fraidey", "fraidei", "salam friday", "salam fida", "cümə", "hey cume",
        "hello friday", "привет фрайдей", "хей фрайдей"
    )

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            handler.post {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                } catch (e: Exception) {
                    Log.e("VoiceSpeechManager", "Init error: ${e.message}")
                }
            }
        }
    }

    fun setHotwordEnabled(enabled: Boolean) {
        _isHotwordEnabled.value = enabled
        if (enabled && _speechState.value == SpeechState.Idle) {
            startHotwordListening()
        } else if (!enabled && _speechState.value == SpeechState.HotwordListening) {
            stopListening()
        }
    }

    fun startListeningForCommand() {
        isExplicitListening = true
        isContinuousListening = false
        startRecognizerInternal(isHotword = false)
    }

    fun startHotwordListening() {
        if (!_isHotwordEnabled.value) return
        isExplicitListening = false
        isContinuousListening = true
        startRecognizerInternal(isHotword = true)
    }

    private fun startRecognizerInternal(isHotword: Boolean) {
        handler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "az-AZ")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                _speechState.value = if (isHotword) SpeechState.HotwordListening else SpeechState.Listening
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Start error: ${e.message}")
                _speechState.value = SpeechState.Error(e.localizedMessage ?: "Mikrofon xətası")
            }
        }
    }

    fun stopListening() {
        isExplicitListening = false
        isContinuousListening = false
        handler.post {
            try {
                speechRecognizer?.stopListening()
                _speechState.value = SpeechState.Idle
                _rmsLevel.value = 0f
            } catch (_: Exception) {}
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _rmsLevel.value = 0f
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB from [-2, 10] to [0.0, 1.0]
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsLevel.value = 0f
            }

            override fun onError(error: Int) {
                _rmsLevel.value = 0f
                if (isContinuousListening && _isHotwordEnabled.value) {
                    // Automatically restart hotword background listening after brief pause
                    handler.postDelayed({
                        if (isContinuousListening && _isHotwordEnabled.value) {
                            startRecognizerInternal(isHotword = true)
                        }
                    }, 1000)
                } else {
                    _speechState.value = SpeechState.Idle
                }
            }

            override fun onResults(results: Bundle?) {
                _rmsLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""

                if (recognizedText.isNotBlank()) {
                    val lower = recognizedText.lowercase().trim()

                    if (isContinuousListening) {
                        // Check if hotword is in recognized text
                        val foundHotword = hotwords.any { lower.contains(it) }
                        if (foundHotword) {
                            val cleanCommand = hotwords.fold(lower) { acc, hw ->
                                acc.replace(hw, "").trim()
                            }
                            _speechState.value = SpeechState.Recognized(recognizedText)
                            onHotwordTriggered()
                            if (cleanCommand.isNotBlank()) {
                                onCommandRecognized(cleanCommand)
                            }
                        } else {
                            // Restart hotword listening
                            handler.postDelayed({
                                if (isContinuousListening && _isHotwordEnabled.value) {
                                    startRecognizerInternal(isHotword = true)
                                }
                            }, 500)
                        }
                    } else {
                        // Explicit command listening mode
                        _speechState.value = SpeechState.Recognized(recognizedText)
                        onCommandRecognized(recognizedText)
                    }
                } else {
                    if (isContinuousListening && _isHotwordEnabled.value) {
                        handler.postDelayed({
                            if (isContinuousListening && _isHotwordEnabled.value) {
                                startRecognizerInternal(isHotword = true)
                            }
                        }, 500)
                    } else {
                        _speechState.value = SpeechState.Idle
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (isContinuousListening && partialText.isNotBlank()) {
                    val lower = partialText.lowercase()
                    if (hotwords.any { lower.contains(it) }) {
                        stopListening()
                        onHotwordTriggered()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }
}
