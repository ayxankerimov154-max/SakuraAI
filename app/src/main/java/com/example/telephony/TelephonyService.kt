package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class TelephonyService(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private val _events = MutableSharedFlow<CommsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<CommsEvent> = _events.asSharedFlow()

    private val _currentIncomingCall = MutableStateFlow<CommsEvent.IncomingCall?>(null)
    val currentIncomingCall: StateFlow<CommsEvent.IncomingCall?> = _currentIncomingCall.asStateFlow()

    companion object {
        @Volatile
        var globalInstance: TelephonyService? = null
    }

    init {
        globalInstance = this
    }

    fun postIncomingCall(number: String, name: String? = null) {
        val event = CommsEvent.IncomingCall(callerNumber = number, callerName = name)
        _currentIncomingCall.value = event
        _events.tryEmit(event)
    }

    fun postIncomingSms(senderNumber: String, messageBody: String, senderName: String? = null) {
        val event = CommsEvent.IncomingSms(
            senderNumber = senderNumber,
            senderName = senderName,
            messageBody = messageBody
        )
        _events.tryEmit(event)
    }

    fun makeCall(phoneNumber: String, directCall: Boolean = true): Boolean {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        if (cleanNumber.isEmpty()) return false

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (directCall && hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("TelephonyService", "Call failed: ${e.message}")
            try {
                val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun sendSms(phoneNumber: String, message: String, directSend: Boolean = true): Boolean {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        if (cleanNumber.isEmpty() || message.isEmpty()) return false

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (directSend && hasSmsPermission) {
            return try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                }
                true
            } catch (e: Exception) {
                Log.e("TelephonyService", "Direct SMS failed: ${e.message}")
                launchSmsIntent(cleanNumber, message)
            }
        } else {
            return launchSmsIntent(cleanNumber, message)
        }
    }

    private fun launchSmsIntent(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("TelephonyService", "SMS Intent failed: ${e.message}")
            false
        }
    }

    fun answerCall(): Boolean {
        _currentIncomingCall.value = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasAnswerPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasAnswerPermission) {
                try {
                    telecomManager?.acceptRingingCall()
                    return true
                } catch (e: Exception) {
                    Log.e("TelephonyService", "Telecom answer failed: ${e.message}")
                }
            }
        }
        return true
    }

    fun rejectCall(): Boolean {
        _currentIncomingCall.value = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                telecomManager?.endCall()
                return true
            } catch (e: Exception) {
                Log.e("TelephonyService", "Telecom reject failed: ${e.message}")
            }
        }
        return true
    }

    fun dismissActiveCallAlert() {
        _currentIncomingCall.value = null
    }
}
