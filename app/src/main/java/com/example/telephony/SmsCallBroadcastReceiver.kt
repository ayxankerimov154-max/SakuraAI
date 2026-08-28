package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.TelephonyManager

class SmsCallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val telephonyService = TelephonyService.globalInstance ?: return

        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNotEmpty()) {
                    val sender = messages[0].displayOriginatingAddress ?: "Gizli Nömrə"
                    val fullBody = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }
                    telephonyService.postIncomingSms(
                        senderNumber = sender,
                        messageBody = fullBody
                    )
                }
            }

            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val number = incomingNumber ?: "Gizli Nömrə"
                    telephonyService.postIncomingCall(number = number)
                }
            }
        }
    }
}
