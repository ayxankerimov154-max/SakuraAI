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
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.VoiceNoteRepository
import com.example.system.SystemSettingsManager
import com.example.system.SystemState
import com.example.telephony.CommsEvent
import com.example.telephony.TelephonyService
import com.example.voice.AudioRecorderPlayer
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

class FridayViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)

    val voiceNoteRepository = VoiceNoteRepository(database.voiceNoteDao(), context)
    val fileManagerRepository = FileManagerRepository(database.fileDocumentDao(), context)
    val assistantLogRepository = AssistantLogRepository(database.assistantLogDao())
    val systemSettingsManager = SystemSettingsManager(context)
    val telephonyService = TelephonyService(context)
    val ttsEngine = TextToSpeechEngine(context)
    val audioRecorderPlayer = AudioRecorderPlayer(context)
    private val geminiService = GeminiService()

    val commandInterpreter = CommandInterpreter(
        systemSettingsManager = systemSettingsManager,
        telephonyService = telephonyService,
        fileManagerRepository = fileManagerRepository,
        voiceNoteRepository = voiceNoteRepository,
        assistantLogRepository = assistantLogRepository,
        ttsEngine = ttsEngine,
        geminiService = geminiService
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

    // UI States
    val systemState: StateFlow<SystemState> = systemSettingsManager.systemState
    val allVoiceNotes: StateFlow<List<VoiceNoteEntity>> = voiceNoteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFiles: StateFlow<List<FileDocumentEntity>> = fileManagerRepository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentLogs: StateFlow<List<AssistantLogEntity>> = assistantLogRepository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechState: StateFlow<SpeechState> = _speechManager.speechState
    val rmsLevel: StateFlow<Float> = _speechManager.rmsLevel
    val isHotwordEnabled: StateFlow<Boolean> = _speechManager.isHotwordEnabled
    val isSpeaking: StateFlow<Boolean> = ttsEngine.isSpeaking

    val isRecordingAudio: StateFlow<Boolean> = audioRecorderPlayer.isRecording
    val recordingDurationMs: StateFlow<Long> = audioRecorderPlayer.recordingDurationMs
    val playbackState: StateFlow<PlaybackState> = audioRecorderPlayer.playbackState

    val incomingCallAlert: StateFlow<CommsEvent.IncomingCall?> = telephonyService.currentIncomingCall

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "FRIDAY",
                text = "Salam! Mən Friday AI köməkçinizəm. Telefon fəaliyyətlərini, səsli qeydləri, faylları və zəngləri idarə etmək üçün hazıram. \"Hey Friday\" deyə və ya mikrofona toxuna bilərsiniz."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isHotwordAwakeModalOpen = MutableStateFlow(false)
    val isHotwordAwakeModalOpen: StateFlow<Boolean> = _isHotwordAwakeModalOpen.asStateFlow()

    private val _editingFile = MutableStateFlow<FileDocumentEntity?>(null)
    val editingFile: StateFlow<FileDocumentEntity?> = _editingFile.asStateFlow()

    init {
        // Observe telephony events for real-time voice announcements
        viewModelScope.launch {
            telephonyService.events.collect { event ->
                when (event) {
                    is CommsEvent.IncomingCall -> {
                        val caller = event.callerName ?: event.callerNumber
                        val announcement = "Diqqət! Gələn zəng: $caller. Qəbul etmək üçün 'Zəngi qəbul et', rədd etmək üçün 'Zəngi rədd et' deyin."
                        ttsEngine.speak(announcement)
                        _chatMessages.update {
                            it + ChatMessage(sender = "FRIDAY", text = announcement, actionType = "INCOMING_CALL")
                        }
                    }
                    is CommsEvent.IncomingSms -> {
                        val sender = event.senderName ?: event.senderNumber
                        val announcement = "Yeni SMS gəldi. Göndərən: $sender. Mesaj: ${event.messageBody}"
                        ttsEngine.speak(announcement)
                        _chatMessages.update {
                            it + ChatMessage(sender = "FRIDAY", text = announcement, actionType = "INCOMING_SMS")
                        }
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
        _speechManager.setHotwordEnabled(enabled)
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
        ttsEngine.speak("Bəli, sizi dinləyirəm?")
        startListening()
    }

    fun dismissAwakeModal() {
        _isHotwordAwakeModalOpen.value = false
    }

    fun processSpokenCommand(rawText: String) {
        _isHotwordAwakeModalOpen.value = false
        if (rawText.isBlank()) return

        _chatMessages.update {
            it + ChatMessage(sender = "USER", text = rawText)
        }

        viewModelScope.launch {
            val result = commandInterpreter.processCommand(rawText)
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
                is ActionResult.GeneralAnswer -> result.answer
            }

            _chatMessages.update {
                it + ChatMessage(
                    sender = "FRIDAY",
                    text = responseText,
                    actionType = when (result) {
                        is ActionResult.Success -> result.actionType
                        is ActionResult.VoiceNoteAction -> "VOICE_NOTE"
                        is ActionResult.CallAction -> "CALL"
                        is ActionResult.SmsAction -> "SMS"
                        is ActionResult.GeneralAnswer -> "AI"
                    }
                )
            }
            systemSettingsManager.refreshState()
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

    override fun onCleared() {
        super.onCleared()
        _speechManager.destroy()
        ttsEngine.shutdown()
        audioRecorderPlayer.release()
    }
}
