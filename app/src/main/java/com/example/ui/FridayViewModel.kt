package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ActionResult
import com.example.ai.CommandInterpreter
import com.example.ai.GeminiService
import com.example.data.db.AppDatabase
import com.example.data.model.AssistantLogEntity
import com.example.data.model.FileDocumentEntity
import com.example.data.model.VoiceNoteEntity
import com.example.data.repository.AssistantLogRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.FridayPreferencesRepository
import com.example.data.repository.VoiceNoteRepository
import com.example.system.SystemSettingsManager
import com.example.system.SystemState
import com.example.telephony.CommsEvent
import com.example.telephony.TelephonyService
import com.example.voice.AudioRecorderPlayer
import com.example.voice.FridayBackgroundVoiceService
import com.example.voice.PlaybackState
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechEngine
import com.example.voice.VoiceSpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "USER" or "FRIDAY"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null
)

enum class LiveTalkPhase {
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    MUTED
}

class FridayViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)

    val preferencesRepository = FridayPreferencesRepository(context)
    val chatRepository = ChatRepository(database.chatMessageDao())
    val voiceNoteRepository = VoiceNoteRepository(database.voiceNoteDao(), context)
    val fileManagerRepository = FileManagerRepository(database.fileDocumentDao(), context)
    val assistantLogRepository = AssistantLogRepository(database.assistantLogDao())
    val systemSettingsManager = SystemSettingsManager(context)
    val telephonyService = TelephonyService(context)
    val ttsEngine = TextToSpeechEngine(context)
    val audioRecorderPlayer = AudioRecorderPlayer(context)
    val geminiService = GeminiService()

    val commandInterpreter = CommandInterpreter(
        systemSettingsManager = systemSettingsManager,
        telephonyService = telephonyService,
        fileManagerRepository = fileManagerRepository,
        voiceNoteRepository = voiceNoteRepository,
        assistantLogRepository = assistantLogRepository,
        ttsEngine = ttsEngine,
        geminiService = geminiService,
        apiKeyProvider = { preferencesRepository.getEffectiveApiKey() }
    )

    private val _speechManager: VoiceSpeechManager = VoiceSpeechManager(
        context = context,
        onHotwordTriggered = {
            handleHotwordAwakening()
        },
        onCommandRecognized = { text ->
            processSpokenCommand(text)
        }
    )

    // UI States from Repositories
    val systemState: StateFlow<SystemState> = systemSettingsManager.systemState
    val allVoiceNotes: StateFlow<List<VoiceNoteEntity>> = voiceNoteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFiles: StateFlow<List<FileDocumentEntity>> = fileManagerRepository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentLogs: StateFlow<List<AssistantLogEntity>> = assistantLogRepository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    val geminiApiKey: StateFlow<String> = preferencesRepository.geminiApiKey
    val isFirstLaunch: StateFlow<Boolean> = preferencesRepository.isFirstLaunch
    val assistantLanguage: StateFlow<String> = preferencesRepository.language
    val isBackgroundListeningEnabled: StateFlow<Boolean> = preferencesRepository.isBackgroundListeningEnabled
    val isSaveHistoryEnabled: StateFlow<Boolean> = preferencesRepository.isSaveHistoryEnabled

    val speechState: StateFlow<SpeechState> = _speechManager.speechState
    val rmsLevel: StateFlow<Float> = _speechManager.rmsLevel
    val partialSpeechText: StateFlow<String> = _speechManager.partialTranscript
    val isHotwordEnabled: StateFlow<Boolean> = preferencesRepository.isHotwordEnabled
    val isSpeaking: StateFlow<Boolean> = ttsEngine.isSpeaking
    val malePitch: StateFlow<Float> = preferencesRepository.voicePitch
    val speechRate: StateFlow<Float> = preferencesRepository.speechRate

    // --- Live Talk (Canlı Söhbət) States ---
    private val _isLiveTalkActive = MutableStateFlow(false)
    val isLiveTalkActive: StateFlow<Boolean> = _isLiveTalkActive.asStateFlow()

    private val _liveTalkPhase = MutableStateFlow(LiveTalkPhase.CONNECTING)
    val liveTalkPhase: StateFlow<LiveTalkPhase> = _liveTalkPhase.asStateFlow()

    private val _isLiveTalkMuted = MutableStateFlow(false)
    val isLiveTalkMuted: StateFlow<Boolean> = _isLiveTalkMuted.asStateFlow()

    private val _liveTalkDurationSeconds = MutableStateFlow(0)
    val liveTalkDurationSeconds: StateFlow<Int> = _liveTalkDurationSeconds.asStateFlow()

    private val _liveTalkLatestUserPrompt = MutableStateFlow("")
    val liveTalkLatestUserPrompt: StateFlow<String> = _liveTalkLatestUserPrompt.asStateFlow()

    private val _liveTalkLatestFridayReply = MutableStateFlow("")
    val liveTalkLatestFridayReply: StateFlow<String> = _liveTalkLatestFridayReply.asStateFlow()

    private var liveTalkTimerJob: kotlinx.coroutines.Job? = null

    private val _isVoiceSettingsOverlayOpen = MutableStateFlow(false)
    val isVoiceSettingsOverlayOpen: StateFlow<Boolean> = _isVoiceSettingsOverlayOpen.asStateFlow()

    val isRecordingAudio: StateFlow<Boolean> = audioRecorderPlayer.isRecording
    val recordingDurationMs: StateFlow<Long> = audioRecorderPlayer.recordingDurationMs
    val playbackState: StateFlow<PlaybackState> = audioRecorderPlayer.playbackState

    val incomingCallAlert: StateFlow<CommsEvent.IncomingCall?> = telephonyService.currentIncomingCall

    private val defaultGreeting = ChatMessage(
        sender = "FRIDAY",
        text = "Salam! Mən Friday (Fida) — çoxdilli şəxsi AI köməkçinizəm. Dünyanın istənilən dilində mənimlə danışa bilərsiniz. Telefon fəaliyyətlərini, səsli qeydləri, faylları və zəngləri idarə etmək üçün \"Hey Friday\" və ya \"Hey Fida\" deyə bilərsiniz."
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(defaultGreeting))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    val chatMessageCount: StateFlow<Int> = chatRepository.messageCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isHotwordAwakeModalOpen = MutableStateFlow(false)
    val isHotwordAwakeModalOpen: StateFlow<Boolean> = _isHotwordAwakeModalOpen.asStateFlow()

    private val _editingFile = MutableStateFlow<FileDocumentEntity?>(null)
    val editingFile: StateFlow<FileDocumentEntity?> = _editingFile.asStateFlow()

    init {
        // Apply persisted voice settings to TTS engine
        ttsEngine.setMaleVoicePitch(preferencesRepository.voicePitch.value)
        ttsEngine.setSpeechRate(preferencesRepository.speechRate.value)
        _speechManager.setHotwordEnabled(preferencesRepository.isHotwordEnabled.value)

        // Setup TTS completion callback for Live Talk continuous turn taking
        ttsEngine.setOnSpeechDoneListener {
            if (_isLiveTalkActive.value && !_isLiveTalkMuted.value) {
                _liveTalkPhase.value = LiveTalkPhase.LISTENING
                viewModelScope.launch {
                    kotlinx.coroutines.delay(200)
                    if (_isLiveTalkActive.value && !_isLiveTalkMuted.value) {
                        _speechManager.resumeLiveTalkListening()
                    }
                }
            }
        }

        // Load conversation history from Room
        viewModelScope.launch {
            chatRepository.allMessages.collect { entities ->
                if (entities.isNotEmpty()) {
                    _chatMessages.value = entities.map {
                        ChatMessage(
                            id = it.id,
                            sender = it.sender,
                            text = it.text,
                            timestamp = it.timestamp,
                            actionType = it.actionType
                        )
                    }
                } else {
                    _chatMessages.value = listOf(defaultGreeting)
                }
            }
        }

        // Sync background service with persisted preference
        if (preferencesRepository.isBackgroundListeningEnabled.value) {
            FridayBackgroundVoiceService.startService(context)
        }

        // Observe telephony events for real-time voice announcements
        viewModelScope.launch {
            telephonyService.events.collect { event ->
                when (event) {
                    is CommsEvent.IncomingCall -> {
                        val caller = event.callerName ?: event.callerNumber
                        val announcement = "Diqqət! Gələn zəng: $caller. Qəbul etmək üçün 'Zəngi qəbul et', rədd etmək üçün 'Zəngi rədd et' deyin."
                        ttsEngine.speak(announcement)
                        appendMessage("FRIDAY", announcement, "INCOMING_CALL")
                    }
                    is CommsEvent.IncomingSms -> {
                        val sender = event.senderName ?: event.senderNumber
                        val announcement = "Yeni SMS gəldi. Göndərən: $sender. Mesaj: ${event.messageBody}"
                        ttsEngine.speak(announcement)
                        appendMessage("FRIDAY", announcement, "INCOMING_SMS")
                    }
                    else -> {}
                }
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleHotword(enabled: Boolean) {
        preferencesRepository.setHotwordEnabled(enabled)
        _speechManager.setHotwordEnabled(enabled)
    }

    fun toggleBackgroundListening(enabled: Boolean) {
        preferencesRepository.setBackgroundListeningEnabled(enabled)
        if (enabled) {
            FridayBackgroundVoiceService.startService(context)
            ttsEngine.speak("Arxa planda dinləmə aktivləşdirildi.")
        } else {
            FridayBackgroundVoiceService.stopService(context)
            ttsEngine.speak("Arxa planda dinləmə dayandırıldı.")
        }
    }

    fun toggleSaveHistory(enabled: Boolean) {
        preferencesRepository.setSaveHistoryEnabled(enabled)
    }

    fun saveGeminiApiKey(key: String) {
        preferencesRepository.setGeminiApiKey(key)
        ttsEngine.speak("Gemini API açarı uğurla yadda saxlanıldı.")
    }

    suspend fun testGeminiConnection(key: String): Result<String> {
        return geminiService.testApiKey(key)
    }

    fun setAssistantLanguage(lang: String) {
        preferencesRepository.setLanguage(lang)
        val confirmation = when (lang) {
            "EN" -> "Language set to English."
            "RU" -> "Язык ассистента установлен на русский."
            "TR" -> "Asistan dili Türkçe olarak ayarlandı."
            else -> "Köməkçi dili Azərbaycan dili olaraq təyin edildi."
        }
        ttsEngine.speak(confirmation)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _chatMessages.value = listOf(defaultGreeting)
            ttsEngine.speak("Söhbət tarixçəsi təmizləndi.")
        }
    }

    fun dismissFirstLaunchModal() {
        preferencesRepository.setFirstLaunchCompleted()
    }

    private fun appendMessage(sender: String, text: String, actionType: String? = null) {
        val newMsg = ChatMessage(
            sender = sender,
            text = text,
            timestamp = System.currentTimeMillis(),
            actionType = actionType
        )
        if (preferencesRepository.isSaveHistoryEnabled.value) {
            viewModelScope.launch {
                chatRepository.saveMessage(sender, text, actionType)
            }
        } else {
            _chatMessages.update { it + newMsg }
        }
    }

    fun startListening() {
        ttsEngine.stop()
        _speechManager.startListeningForCommand()
    }

    fun stopListening() {
        _speechManager.stopListening()
    }

    private fun handleHotwordAwakening() {
        _isHotwordAwakeModalOpen.value = true
        val response = when (preferencesRepository.language.value) {
            "EN" -> "Yes, sir, I am listening."
            "RU" -> "Да, сэр, слушаю вас."
            "TR" -> "Evet, sizi dinliyorum."
            else -> "Bəli, sizi dinləyirəm?"
        }
        ttsEngine.speak(response)
        startListening()
    }

    fun dismissAwakeModal() {
        _isHotwordAwakeModalOpen.value = false
    }

    fun processSpokenCommand(rawText: String) {
        _isHotwordAwakeModalOpen.value = false
        if (rawText.isBlank()) return

        _liveTalkLatestUserPrompt.value = rawText
        if (_isLiveTalkActive.value) {
            _liveTalkPhase.value = LiveTalkPhase.THINKING
            _speechManager.pauseLiveTalkListening()
        }

        appendMessage("USER", rawText)

        viewModelScope.launch {
            val result = commandInterpreter.processCommand(rawText, preferencesRepository.getEffectiveApiKey())
            val responseText = when (result) {
                is ActionResult.Success -> result.message
                is ActionResult.VoiceNoteAction -> {
                    if (result.startRecording) {
                        startVoiceRecording("Səsli Qeyd ${System.currentTimeMillis() % 1000}")
                    }
                    result.message
                }
                is ActionResult.CallAction -> result.message
                is ActionResult.SmsAction -> result.message
                is ActionResult.LiveTalkAction -> {
                    if (result.startLiveTalk && !_isLiveTalkActive.value) {
                        startLiveTalk()
                    }
                    result.message
                }
                is ActionResult.GeneralAnswer -> result.answer
            }

            val actionType = when (result) {
                is ActionResult.Success -> result.actionType
                is ActionResult.VoiceNoteAction -> "VOICE_NOTE"
                is ActionResult.CallAction -> "CALL"
                is ActionResult.SmsAction -> "SMS"
                is ActionResult.LiveTalkAction -> "LIVE_TALK"
                is ActionResult.GeneralAnswer -> "AI"
            }

            _liveTalkLatestFridayReply.value = responseText
            if (_isLiveTalkActive.value) {
                _liveTalkPhase.value = LiveTalkPhase.SPEAKING
            }

            appendMessage("FRIDAY", responseText, actionType)
            systemSettingsManager.refreshState()
        }
    }

    // --- Live Talk (Canlı Söhbət) Management ---
    fun startLiveTalk() {
        if (_isLiveTalkActive.value) return
        _isLiveTalkActive.value = true
        _isLiveTalkMuted.value = false
        _liveTalkPhase.value = LiveTalkPhase.CONNECTING
        _liveTalkDurationSeconds.value = 0
        _liveTalkLatestUserPrompt.value = ""
        _liveTalkLatestFridayReply.value = "Salam! Canlı söhbət rejimi aktivdir. Friday xidmətinizdədir, buyurun danışın."

        // Start call duration timer
        liveTalkTimerJob?.cancel()
        liveTalkTimerJob = viewModelScope.launch {
            while (_isLiveTalkActive.value) {
                kotlinx.coroutines.delay(1000)
                _liveTalkDurationSeconds.update { it + 1 }
            }
        }

        val greeting = when (preferencesRepository.language.value) {
            "EN" -> "Live talk connected. Friday is online, I am listening."
            "RU" -> "Режим живого разговора включён. Фрайдей на связи, слушаю вас."
            "TR" -> "Canlı konuşma modu aktif. Friday hazır, sizi dinliyorum."
            else -> "Canlı söhbət aktivləşdirildi. Friday sizi dinləyir, buyurun."
        }

        _liveTalkPhase.value = LiveTalkPhase.SPEAKING
        ttsEngine.speak(greeting)
    }

    fun endLiveTalk() {
        _isLiveTalkActive.value = false
        liveTalkTimerJob?.cancel()
        liveTalkTimerJob = null
        _speechManager.stopLiveTalkListening()
        ttsEngine.stop()
        _liveTalkPhase.value = LiveTalkPhase.CONNECTING
        _liveTalkDurationSeconds.value = 0
    }

    fun toggleLiveTalkMute() {
        if (!_isLiveTalkActive.value) return
        val newMute = !_isLiveTalkMuted.value
        _isLiveTalkMuted.value = newMute
        _speechManager.setLiveTalkMuted(newMute)
        if (newMute) {
            _liveTalkPhase.value = LiveTalkPhase.MUTED
            ttsEngine.stop()
        } else {
            _liveTalkPhase.value = LiveTalkPhase.LISTENING
        }
    }

    fun interruptLiveTalkSpeaking() {
        if (!_isLiveTalkActive.value) return
        ttsEngine.stop()
        _liveTalkPhase.value = LiveTalkPhase.LISTENING
        if (!_isLiveTalkMuted.value) {
            _speechManager.resumeLiveTalkListening()
        }
    }

    fun sendTextMessage(text: String) {
        processSpokenCommand(text)
    }

    // --- Voice Notes Operations ---
    fun startVoiceRecording(title: String = "") {
        val audioDir = voiceNoteRepository.getAudioDirectory()
        val tempFile = File(audioDir, "recording_${System.currentTimeMillis()}.m4a")
        audioRecorderPlayer.startRecording(tempFile)
    }

    fun stopVoiceRecording(title: String = "Səsli Qeyd") {
        viewModelScope.launch {
            val duration = audioRecorderPlayer.recordingDurationMs.value
            val file = audioRecorderPlayer.stopRecording()
            if (file != null && file.exists()) {
                voiceNoteRepository.saveVoiceNote(
                    title = title.ifBlank { "Səsli Qeyd ${System.currentTimeMillis() % 10000}" },
                    file = file,
                    durationMs = duration,
                    transcript = ""
                )
                ttsEngine.speak("Səsli qeyd uğurla saxlanıldı.")
            }
        }
    }

    fun cancelVoiceRecording() {
        audioRecorderPlayer.cancelRecording()
    }

    fun playVoiceNote(note: VoiceNoteEntity) {
        audioRecorderPlayer.playNote(note.id, note.filePath)
    }

    fun pauseVoiceNote() {
        audioRecorderPlayer.pausePlayback()
    }

    fun seekVoiceNote(posMs: Int) {
        audioRecorderPlayer.seekTo(posMs)
    }

    fun stopVoicePlayback() {
        audioRecorderPlayer.stopPlayback()
    }

    fun deleteVoiceNote(note: VoiceNoteEntity) {
        viewModelScope.launch {
            if (playbackState.value.activeNoteId == note.id) {
                audioRecorderPlayer.stopPlayback()
            }
            voiceNoteRepository.deleteVoiceNote(note)
            ttsEngine.speak("Səsli qeyd silindi.")
        }
    }

    // --- File Operations ---
    fun createFile(name: String, content: String, extension: String = "txt") {
        viewModelScope.launch {
            fileManagerRepository.createFile(name, content, extension)
            ttsEngine.speak("$name faylı yaradıldı.")
        }
    }

    fun openFileEditor(file: FileDocumentEntity) {
        _editingFile.value = file
    }

    fun closeFileEditor() {
        _editingFile.value = null
    }

    fun saveFileEdit(newContent: String) {
        val file = _editingFile.value ?: return
        viewModelScope.launch {
            fileManagerRepository.updateFile(file.id, newContent)
            _editingFile.value = null
            ttsEngine.speak("Fayl yeniləndi.")
        }
    }

    fun deleteFile(file: FileDocumentEntity) {
        viewModelScope.launch {
            fileManagerRepository.deleteFile(file)
            ttsEngine.speak("Fayl silindi.")
        }
    }

    // --- Hardware & Widget Settings ---
    fun toggleWifi() {
        systemSettingsManager.toggleWifi()
    }

    fun toggleBluetooth() {
        systemSettingsManager.toggleBluetooth()
    }

    fun toggleFlashlight() {
        systemSettingsManager.toggleFlashlight()
    }

    fun setMediaVolume(percent: Int) {
        systemSettingsManager.setMediaVolume(percent)
    }

    fun setRingVolume(percent: Int) {
        systemSettingsManager.setRingVolume(percent)
    }

    fun setAlarmVolume(percent: Int) {
        systemSettingsManager.setAlarmVolume(percent)
    }

    fun setBrightness(percent: Int) {
        systemSettingsManager.setBrightness(percent)
    }

    fun setRingerMode(mode: Int) {
        systemSettingsManager.setRingerMode(mode)
    }

    // --- Telephony & Messaging ---
    fun makeCall(phoneNumber: String, direct: Boolean = true) {
        telephonyService.makeCall(phoneNumber, direct)
    }

    fun sendSms(phoneNumber: String, message: String, direct: Boolean = true) {
        telephonyService.sendSms(phoneNumber, message, direct)
    }

    fun answerIncomingCall() {
        telephonyService.answerCall()
        ttsEngine.speak("Zəng qəbul olundu.")
    }

    fun rejectIncomingCall() {
        telephonyService.rejectCall()
        ttsEngine.speak("Zəng rədd olundu.")
    }

    fun simulateIncomingCall(callerName: String, callerNumber: String) {
        telephonyService.postIncomingCall(callerNumber, callerName)
    }

    fun simulateIncomingSms(senderName: String, senderNumber: String, message: String) {
        telephonyService.postIncomingSms(senderNumber, message, senderName)
    }

    fun speakText(text: String) {
        ttsEngine.speak(text)
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun openVoiceSettingsOverlay() {
        _isVoiceSettingsOverlayOpen.value = true
    }

    fun closeVoiceSettingsOverlay() {
        _isVoiceSettingsOverlayOpen.value = false
    }

    fun setMaleVoicePitch(pitch: Float) {
        preferencesRepository.setVoicePitch(pitch)
        ttsEngine.setMaleVoicePitch(pitch)
    }

    fun setSpeechRate(rate: Float) {
        preferencesRepository.setSpeechRate(rate)
        ttsEngine.setSpeechRate(rate)
    }

    fun resetVoiceDefaults() {
        preferencesRepository.resetVoiceDefaults()
        ttsEngine.resetVoiceDefaults()
    }

    fun testVoiceSettings(sampleText: String = "Salam! Səs və sürət parametrləri uğurla tətbiq edildi. Friday xidmətinizdədir.") {
        ttsEngine.stop()
        ttsEngine.speak(sampleText)
    }

    override fun onCleared() {
        super.onCleared()
        _speechManager.destroy()
        ttsEngine.shutdown()
        audioRecorderPlayer.release()
    }
}
