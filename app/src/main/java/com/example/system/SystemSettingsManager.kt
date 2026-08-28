package com.example.system

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemState(
    val isWifiEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val mediaVolumePercent: Int = 50,
    val ringVolumePercent: Int = 70,
    val alarmVolumePercent: Int = 80,
    val brightnessPercent: Int = 60,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL, // 0=SILENT, 1=VIBRATE, 2=NORMAL
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false
)

class SystemSettingsManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val bluetoothAdapter: BluetoothAdapter? = try {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    } catch (_: Exception) {
        null
    }

    private val _systemState = MutableStateFlow(fetchCurrentState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        findCameraWithFlash()
        refreshState()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    return
                } else if (hasFlash && cameraIdWithFlash == null) {
                    cameraIdWithFlash = id
                }
            }
        } catch (e: Exception) {
            Log.e("SystemSettings", "Error finding camera flash: ${e.message}")
        }
    }

    fun fetchCurrentState(): SystemState {
        val isWifi = try {
            wifiManager?.isWifiEnabled ?: false
        } catch (_: Exception) {
            false
        }

        val isBt = try {
            bluetoothAdapter?.isEnabled ?: false
        } catch (_: Exception) {
            false
        }

        val mediaVol = audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curr = it.getStreamVolume(AudioManager.STREAM_MUSIC)
            (curr * 100) / max
        } ?: 50

        val ringVol = audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
            val curr = it.getStreamVolume(AudioManager.STREAM_RING)
            (curr * 100) / max
        } ?: 70

        val alarmVol = audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
            val curr = it.getStreamVolume(AudioManager.STREAM_ALARM)
            (curr * 100) / max
        } ?: 80

        val brightness = try {
            val curBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            (curBrightness * 100) / 255
        } catch (_: Exception) {
            60
        }

        val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            batteryManager?.isCharging ?: false
        } else {
            false
        }

        return SystemState(
            isWifiEnabled = isWifi,
            isBluetoothEnabled = isBt,
            isFlashlightOn = _systemState.value.isFlashlightOn,
            mediaVolumePercent = mediaVol,
            ringVolumePercent = ringVol,
            alarmVolumePercent = alarmVol,
            brightnessPercent = brightness,
            ringerMode = ringer,
            batteryPercent = batteryPct,
            isCharging = isCharging
        )
    }

    fun refreshState() {
        _systemState.value = fetchCurrentState()
    }

    // --- Wi-Fi Control ---
    fun toggleWifi(enable: Boolean? = null): Boolean {
        val target = enable ?: !_systemState.value.isWifiEnabled
        var success = false
        try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                success = wifiManager?.setWifiEnabled(target) ?: false
            } else {
                // On Android 10+, apps cannot directly toggle wifi silently without system settings
                openWifiSettings()
                success = true
            }
        } catch (e: Exception) {
            openWifiSettings()
            success = false
        }
        _systemState.value = _systemState.value.copy(isWifiEnabled = target)
        return success
    }

    fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // --- Bluetooth Control ---
    fun toggleBluetooth(enable: Boolean? = null): Boolean {
        val target = enable ?: !_systemState.value.isBluetoothEnabled
        try {
            @Suppress("DEPRECATION")
            if (target) {
                bluetoothAdapter?.enable()
            } else {
                bluetoothAdapter?.disable()
            }
        } catch (e: Exception) {
            openBluetoothSettings()
        }
        _systemState.value = _systemState.value.copy(isBluetoothEnabled = target)
        return true
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // --- Flashlight Control ---
    fun toggleFlashlight(enable: Boolean? = null): Boolean {
        val target = enable ?: !_systemState.value.isFlashlightOn
        val camId = cameraIdWithFlash ?: return false
        return try {
            cameraManager?.setTorchMode(camId, target)
            _systemState.value = _systemState.value.copy(isFlashlightOn = target)
            true
        } catch (e: CameraAccessException) {
            Log.e("SystemSettings", "Torch error: ${e.message}")
            false
        }
    }

    // --- Volume Controls ---
    fun setMediaVolume(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val index = (clamped * max) / 100
            it.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_SHOW_UI)
        }
        _systemState.value = _systemState.value.copy(mediaVolumePercent = clamped)
    }

    fun setRingVolume(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_RING)
            val index = (clamped * max) / 100
            it.setStreamVolume(AudioManager.STREAM_RING, index, AudioManager.FLAG_SHOW_UI)
        }
        _systemState.value = _systemState.value.copy(ringVolumePercent = clamped)
    }

    fun setAlarmVolume(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val index = (clamped * max) / 100
            it.setStreamVolume(AudioManager.STREAM_ALARM, index, AudioManager.FLAG_SHOW_UI)
        }
        _systemState.value = _systemState.value.copy(alarmVolumePercent = clamped)
    }

    // --- Screen Brightness ---
    fun setBrightness(percent: Int): Boolean {
        val clamped = percent.coerceIn(0, 100)
        val value255 = (clamped * 255) / 100
        var success = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                try {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        value255
                    )
                    success = true
                } catch (e: Exception) {
                    Log.e("SystemSettings", "Brightness error: ${e.message}")
                }
            } else {
                openWriteSettingsPermission()
            }
        } else {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    value255
                )
                success = true
            } catch (_: Exception) {}
        }

        _systemState.value = _systemState.value.copy(brightnessPercent = clamped)
        return success
    }

    fun openWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    // --- Ringer Mode ---
    fun setRingerMode(mode: Int) {
        audioManager?.let {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || nm?.isNotificationPolicyAccessGranted == true) {
                    it.ringerMode = mode
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE)) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("SystemSettings", "Ringer mode error: ${e.message}")
            }
        }
        _systemState.value = _systemState.value.copy(ringerMode = mode)
    }

    fun openDisplaySettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openSoundSettings() {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
