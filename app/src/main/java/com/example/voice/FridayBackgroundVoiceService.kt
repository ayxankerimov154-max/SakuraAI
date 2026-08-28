package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ai.ActionResult
import com.example.ai.CommandInterpreter
import com.example.ai.GeminiService
import com.example.data.db.AppDatabase
import com.example.data.repository.AssistantLogRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.FridayPreferencesRepository
import com.example.data.repository.VoiceNoteRepository
import com.example.system.SystemSettingsManager
import com.example.telephony.TelephonyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FridayBackgroundVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceSpeechManager: VoiceSpeechManager? = null
    private var ttsEngine: TextToSpeechEngine? = null
    private var commandInterpreter: CommandInterpreter? = null
    private var preferencesRepository: FridayPreferencesRepository? = null
    private var chatRepository: ChatRepository? = null

    companion object {
        const val CHANNEL_ID = "friday_voice_background_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_VOICE_SERVICE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FridayBackgroundVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FridayBackgroundVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true

        val database = AppDatabase.getDatabase(applicationContext)
        val systemSettingsManager = SystemSettingsManager(applicationContext)
        val telephonyService = TelephonyService(applicationContext)
        val fileManagerRepo = FileManagerRepository(database.fileDocumentDao(), applicationContext)
        val voiceNoteRepo = VoiceNoteRepository(database.voiceNoteDao(), applicationContext)
        val logRepo = AssistantLogRepository(database.assistantLogDao())
        chatRepository = ChatRepository(database.chatMessageDao())
        preferencesRepository = FridayPreferencesRepository(applicationContext)

        val geminiService = GeminiService()
        ttsEngine = TextToSpeechEngine(applicationContext).apply {
            preferencesRepository?.let { prefs ->
                setMaleVoicePitch(prefs.voicePitch.value)
                setSpeechRate(prefs.speechRate.value)
            }
        }

        commandInterpreter = CommandInterpreter(
            systemSettingsManager = systemSettingsManager,
            telephonyService = telephonyService,
            fileManagerRepository = fileManagerRepo,
            voiceNoteRepository = voiceNoteRepo,
            assistantLogRepository = logRepo,
            ttsEngine = ttsEngine!!,
            geminiService = geminiService,
            apiKeyProvider = { preferencesRepository?.getEffectiveApiKey() ?: "" }
        )

        voiceSpeechManager = VoiceSpeechManager(
            context = applicationContext,
            onHotwordTriggered = {
                handleBackgroundHotword()
            },
            onCommandRecognized = { text ->
                handleBackgroundCommand(text)
            }
        )

        createNotificationChannel()
        startInForeground()

        // Start listening for hotword
        voiceSpeechManager?.setHotwordEnabled(true)
        voiceSpeechManager?.startHotwordListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun handleBackgroundHotword() {
        Log.d("FridayBackgroundService", "Hotword detected in background")
        ttsEngine?.speak("Bəli, sizi dinləyirəm?")
        voiceSpeechManager?.startListeningForCommand()
    }

    private fun handleBackgroundCommand(text: String) {
        if (text.isBlank()) return
        Log.d("FridayBackgroundService", "Command recognized in background: $text")

        serviceScope.launch {
            chatRepository?.saveMessage(sender = "USER", text = text)

            val result = commandInterpreter?.processCommand(text)
            val responseText = when (result) {
                is ActionResult.Success -> result.message
                is ActionResult.VoiceNoteAction -> result.message
                is ActionResult.CallAction -> result.message
                is ActionResult.SmsAction -> result.message
                is ActionResult.GeneralAnswer -> result.answer
                null -> "Əmr icra edildi."
            }

            chatRepository?.saveMessage(
                sender = "FRIDAY",
                text = responseText,
                actionType = "BACKGROUND_VOICE"
            )

            // Update notification with latest interaction
            updateNotification("Son Əmr: \"$text\"", responseText)

            // Resume background hotword listening
            voiceSpeechManager?.startHotwordListening()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Friday Arxa Plan Səs Xidməti",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Friday 'Hey Friday' oyanma sözünü arxa planda dinləyir."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startInForeground() {
        val notification = buildNotification("Friday Köməkçi Aktivdir", "'Friday' və ya 'Hey Friday' deyə bilərsiniz.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FridayBackgroundVoiceService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dayandır", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        voiceSpeechManager?.stopListening()
        voiceSpeechManager?.destroy()
        ttsEngine?.shutdown()
        serviceScope.cancel()
    }
}
