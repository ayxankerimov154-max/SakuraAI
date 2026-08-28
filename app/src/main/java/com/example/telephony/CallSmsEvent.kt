package com.example.telephony

sealed class CommsEvent {
    data class IncomingCall(
        val callerNumber: String,
        val callerName: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : CommsEvent()

    data class IncomingSms(
        val senderNumber: String,
        val senderName: String? = null,
        val messageBody: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CommsEvent()

    data class CallStateChanged(
        val state: String // "RINGING", "OFFHOOK", "IDLE"
    ) : CommsEvent()
}
