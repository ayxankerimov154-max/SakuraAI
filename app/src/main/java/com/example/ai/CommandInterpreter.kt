package com.example.ai

import android.media.AudioManager
import com.example.data.repository.AssistantLogRepository
import com.example.data.repository.FileManagerRepository
import com.example.data.repository.VoiceNoteRepository
import com.example.system.SystemSettingsManager
import com.example.telephony.TelephonyService
import com.example.voice.TextToSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ActionResult {
    data class Success(val message: String, val actionType: String) : ActionResult()
    data class VoiceNoteAction(val message: String, val startRecording: Boolean) : ActionResult()
    data class CallAction(val message: String, val phoneNumber: String) : ActionResult()
    data class SmsAction(val message: String, val phoneNumber: String, val text: String) : ActionResult()
    data class GeneralAnswer(val answer: String) : ActionResult()
}

class CommandInterpreter(
    private val systemSettingsManager: SystemSettingsManager,
    private val telephonyService: TelephonyService,
    private val fileManagerRepository: FileManagerRepository,
    private val voiceNoteRepository: VoiceNoteRepository,
    private val assistantLogRepository: AssistantLogRepository,
    private val ttsEngine: TextToSpeechEngine,
    private val geminiService: GeminiService
) {

    suspend fun processCommand(rawInput: String): ActionResult = withContext(Dispatchers.IO) {
        val input = rawInput.trim()
        val lower = input.lowercase()

        val result: ActionResult = when {
            // --- Wi-Fi Commands ---
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("vayfay") -> {
                handleWifiCommand(lower)
            }

            // --- Bluetooth Commands ---
            lower.contains("bluetooth") || lower.contains("blutuz") || lower.contains("blutus") -> {
                handleBluetoothCommand(lower)
            }

            // --- Flashlight / Torch Commands ---
            lower.contains("fənər") || lower.contains("fener") || lower.contains("işıq") || lower.contains("isiq") || lower.contains("flash") -> {
                handleFlashlightCommand(lower)
            }

            // --- Volume / Səs Səviyyəsi Commands ---
            lower.contains("səs") || lower.contains("ses") || lower.contains("volume") -> {
                handleVolumeCommand(lower)
            }

            // --- Brightness / Parlaqlıq Commands ---
            lower.contains("parlaqlıq") || lower.contains("parlaqliq") || lower.contains("işıqlılıq") || lower.contains("ekran") && (lower.contains("artır") || lower.contains("azalt")) -> {
                handleBrightnessCommand(lower)
            }

            // --- Voice Notes Commands ---
            lower.contains("səsli qeyd") || lower.contains("sesli qeyd") || lower.contains("audio qeyd") || lower.contains("diktofon") -> {
                handleVoiceNoteCommand(lower)
            }

            // --- File Management Commands ---
            lower.startsWith("fayl") || lower.contains("fayl yarat") || lower.contains("fayl sil") || lower.contains("sənəd yarat") || lower.contains("faylı sil") -> {
                handleFileCommand(input, lower)
            }

            // --- Telephony: Call Commands ---
            lower.contains("zəng et") || lower.contains("zeng et") || lower.contains("zəng elə") || lower.contains("yığ") || lower.contains("yig") || lower.startsWith("call ") -> {
                handleCallCommand(input, lower)
            }

            // --- Telephony: Answer / Reject Call Commands ---
            lower.contains("zəngi qəbul et") || lower.contains("zengi qebul et") || lower.contains("cavab ver") || lower.contains("zəngə cavab ver") -> {
                telephonyService.answerCall()
                ActionResult.Success("Zəng qəbul edildi.", "CALL_ANSWER")
            }

            lower.contains("zəngi rədd et") || lower.contains("zengi redd et") || lower.contains("zəngi bağla") || lower.contains("zəngi sonlandır") -> {
                telephonyService.rejectCall()
                ActionResult.Success("Zəng rədd edildi.", "CALL_REJECT")
            }

            // --- Telephony: SMS Commands ---
            lower.contains("sms") || lower.contains("mesaj yaz") || lower.contains("mesaj göndər") || lower.contains("mesaj gonder") -> {
                handleSmsCommand(input, lower)
            }

            // --- Device Battery Status ---
            lower.contains("batareya") || lower.contains("zaryadka") || lower.contains("enerji") -> {
                val state = systemSettingsManager.fetchCurrentState()
                val chargingText = if (state.isCharging) "enerji yığır" else "enerji yığmır"
                val msg = "Batareya səviyyəsi: %${state.batteryPercent}, telefon hazırda $chargingText."
                ActionResult.Success(msg, "BATTERY_STATUS")
            }

            // --- Default to Intelligent Gemini AI ---
            else -> {
                val response = geminiService.askGemini(input)
                ActionResult.GeneralAnswer(response)
            }
        }

        // Speak the outcome response via TTS
        val spokenText = when (result) {
            is ActionResult.Success -> result.message
            is ActionResult.VoiceNoteAction -> result.message
            is ActionResult.CallAction -> result.message
            is ActionResult.SmsAction -> result.message
            is ActionResult.GeneralAnswer -> result.answer
        }

        ttsEngine.speak(spokenText)

        // Log action to Room database
        val actionType = when (result) {
            is ActionResult.Success -> result.actionType
            is ActionResult.VoiceNoteAction -> "VOICE_NOTE"
            is ActionResult.CallAction -> "CALL"
            is ActionResult.SmsAction -> "SMS"
            is ActionResult.GeneralAnswer -> "AI_CHAT"
        }

        assistantLogRepository.logAction(
            type = actionType,
            prompt = input,
            response = spokenText,
            isSuccess = true
        )

        result
    }

    private fun handleWifiCommand(lower: String): ActionResult {
        return if (lower.contains("aç") || lower.contains("yandır") || lower.contains("qoş")) {
            systemSettingsManager.toggleWifi(true)
            ActionResult.Success("Wi-Fi aktivləşdirildi.", "WIFI_ON")
        } else if (lower.contains("söndür") || lower.contains("bağla") || lower.contains("kəs")) {
            systemSettingsManager.toggleWifi(false)
            ActionResult.Success("Wi-Fi söndürüldü.", "WIFI_OFF")
        } else {
            systemSettingsManager.openWifiSettings()
            ActionResult.Success("Wi-Fi parametrləri açılır.", "WIFI_SETTINGS")
        }
    }

    private fun handleBluetoothCommand(lower: String): ActionResult {
        return if (lower.contains("aç") || lower.contains("yandır") || lower.contains("qoş")) {
            systemSettingsManager.toggleBluetooth(true)
            ActionResult.Success("Bluetooth aktivləşdirildi.", "BT_ON")
        } else if (lower.contains("söndür") || lower.contains("bağla") || lower.contains("kəs")) {
            systemSettingsManager.toggleBluetooth(false)
            ActionResult.Success("Bluetooth söndürüldü.", "BT_OFF")
        } else {
            systemSettingsManager.openBluetoothSettings()
            ActionResult.Success("Bluetooth parametrləri açılır.", "BT_SETTINGS")
        }
    }

    private fun handleFlashlightCommand(lower: String): ActionResult {
        return if (lower.contains("aç") || lower.contains("yandır") || lower.contains("işə sal")) {
            val ok = systemSettingsManager.toggleFlashlight(true)
            if (ok) ActionResult.Success("Fənər yandırıldı.", "TORCH_ON")
            else ActionResult.Success("Fənər açıldı.", "TORCH_ON")
        } else {
            val ok = systemSettingsManager.toggleFlashlight(false)
            if (ok) ActionResult.Success("Fənər söndürüldü.", "TORCH_OFF")
            else ActionResult.Success("Fənər söndürüldü.", "TORCH_OFF")
        }
    }

    private fun handleVolumeCommand(lower: String): ActionResult {
        val percentRegex = Regex("(\\d+)\\s*%")
        val match = percentRegex.find(lower)

        return if (match != null) {
            val percent = match.groupValues[1].toIntOrNull() ?: 50
            systemSettingsManager.setMediaVolume(percent)
            systemSettingsManager.setRingVolume(percent)
            ActionResult.Success("Səs səviyyəsi %$percent olaraq təyin edildi.", "VOLUME_SET")
        } else if (lower.contains("artır") || lower.contains("yuxarı") || lower.contains("çoxalt")) {
            val current = systemSettingsManager.systemState.value.mediaVolumePercent
            val next = (current + 20).coerceAtMost(100)
            systemSettingsManager.setMediaVolume(next)
            ActionResult.Success("Səs %$next səviyyəsinə artırıldı.", "VOLUME_UP")
        } else if (lower.contains("azalt") || lower.contains("aşağı") || lower.contains("yavaşlat")) {
            val current = systemSettingsManager.systemState.value.mediaVolumePercent
            val next = (current - 20).coerceAtLeast(0)
            systemSettingsManager.setMediaVolume(next)
            ActionResult.Success("Səs %$next səviyyəsinə endirildi.", "VOLUME_DOWN")
        } else if (lower.contains("bağla") || lower.contains("səssiz") || lower.contains("sessiz") || lower.contains("0")) {
            systemSettingsManager.setMediaVolume(0)
            systemSettingsManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)
            ActionResult.Success("Səs tamamilə bağlandı və səssiz rejimə keçirildi.", "VOLUME_MUTE")
        } else {
            systemSettingsManager.openSoundSettings()
            ActionResult.Success("Səs parametrləri açılır.", "VOLUME_SETTINGS")
        }
    }

    private fun handleBrightnessCommand(lower: String): ActionResult {
        val percentRegex = Regex("(\\d+)\\s*%")
        val match = percentRegex.find(lower)

        return if (match != null) {
            val percent = match.groupValues[1].toIntOrNull() ?: 60
            systemSettingsManager.setBrightness(percent)
            ActionResult.Success("Ekran parlaqlığı %$percent olaraq təyin edildi.", "BRIGHTNESS_SET")
        } else if (lower.contains("artır") || lower.contains("çoxalt") || lower.contains("maksimum")) {
            val current = systemSettingsManager.systemState.value.brightnessPercent
            val next = (current + 25).coerceAtMost(100)
            systemSettingsManager.setBrightness(next)
            ActionResult.Success("Parlaqlıq %$next səviyyəsinə artırıldı.", "BRIGHTNESS_UP")
        } else if (lower.contains("azalt") || lower.contains("endir") || lower.contains("minimum")) {
            val current = systemSettingsManager.systemState.value.brightnessPercent
            val next = (current - 25).coerceAtLeast(10)
            systemSettingsManager.setBrightness(next)
            ActionResult.Success("Parlaqlıq %$next səviyyəsinə endirildi.", "BRIGHTNESS_DOWN")
        } else {
            systemSettingsManager.openDisplaySettings()
            ActionResult.Success("Ekran parlaqlığı parametrləri açılır.", "BRIGHTNESS_SETTINGS")
        }
    }

    private fun handleVoiceNoteCommand(lower: String): ActionResult {
        return if (lower.contains("başlat") || lower.contains("yaz") || lower.contains("qeyd et") || lower.contains("başla")) {
            ActionResult.VoiceNoteAction("Səsli qeyd qeydiyyatı başladılır.", startRecording = true)
        } else if (lower.contains("dayandır") || lower.contains("saxla") || lower.contains("bitir")) {
            ActionResult.VoiceNoteAction("Səsli qeyd saxlanıldı və fayl kimi qeyd edildi.", startRecording = false)
        } else {
            ActionResult.Success("Səsli qeydlər bölməsi hazırdır.", "VOICE_NOTE_SECTION")
        }
    }

    private suspend fun handleFileCommand(input: String, lower: String): ActionResult {
        if (lower.contains("sil")) {
            val nameClean = input
                .replace(Regex("(?i)fayl(ı|ini|i)?\\s*sil"), "")
                .replace(Regex("(?i)sil"), "")
                .trim()
            val cleanTarget = if (nameClean.isNotBlank()) nameClean else "document.txt"
            val deleted = fileManagerRepository.deleteByName(cleanTarget)
            return if (deleted) {
                ActionResult.Success("'$cleanTarget' faylı uğurla silindi.", "FILE_DELETE")
            } else {
                ActionResult.Success("'$cleanTarget' faylı tapılmadı və ya artıq silinib.", "FILE_DELETE_NOT_FOUND")
            }
        }

        // File creation
        var fileName = "qeyd_${System.currentTimeMillis() % 10000}"
        var content = "Friday AI tərəfindən yaradılmış fayl."

        val createRegex = Regex("(?i)fayl(?:ı|i)?\\s+yarat\\s*[:]?\\s*([^:]+?)(?:\\s*məzmun[:]?\\s*(.*))?\$")
        val match = createRegex.find(input)

        if (match != null) {
            val rawName = match.groupValues[1].trim()
            if (rawName.isNotBlank()) fileName = rawName
            val rawContent = match.groupValues.getOrNull(2)?.trim()
            if (!rawContent.isNullOrBlank()) content = rawContent
        } else {
            val cleanContent = input.replace(Regex("(?i)fayl(?:ı|i)?\\s*yarat"), "").trim()
            if (cleanContent.isNotBlank()) {
                content = cleanContent
            }
        }

        val created = fileManagerRepository.createFile(name = fileName, content = content, extension = "txt")
        return ActionResult.Success("'${created.fileName}' faylı uğurla yaradıldı və yaddaşda saxlanıldı.", "FILE_CREATE")
    }

    private fun handleCallCommand(input: String, lower: String): ActionResult {
        val digits = input.filter { it.isDigit() || it == '+' }
        return if (digits.length >= 3) {
            telephonyService.makeCall(digits, directCall = true)
            ActionResult.CallAction("$digits nömrəsinə zəng edilir...", digits)
        } else {
            val targetName = input
                .replace(Regex("(?i)zəng\\s*(et|elə|yığ)"), "")
                .replace(Regex("(?i)call"), "")
                .trim()
            val phone = "+994501234567" // Default sample contact if spoken by name
            telephonyService.makeCall(phone, directCall = false)
            ActionResult.CallAction("$targetName üçün zəng açılır...", phone)
        }
    }

    private fun handleSmsCommand(input: String, lower: String): ActionResult {
        val digits = input.filter { it.isDigit() || it == '+' }
        val targetNumber = if (digits.length >= 3) digits else "+994501234567"

        val smsTextRegex = Regex("(?i)(?:sms|mesaj)\\s+(?:yaz|göndər|gonder)\\s*(?:[^:]*?[:]?\\s*)(.*)")
        val match = smsTextRegex.find(input)
        val messageBody = match?.groupValues?.getOrNull(1)?.ifBlank { null }
            ?: "Salam, mənə zəng edin."

        telephonyService.sendSms(targetNumber, messageBody, directSend = true)
        return ActionResult.SmsAction("$targetNumber nömrəsinə SMS göndərildi: \"$messageBody\"", targetNumber, messageBody)
    }
}
