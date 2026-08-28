package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemInstruction = """
        Sən 'Friday' adında yüksək intellektli, nəzakətli və cəld Android AI səs köməkçisisən.
        İstifadəçinin suallarına aydın, lakonik, dəqiq və təbii danışıq dilində (əsasən Azərbaycan dilində) cavab verirsən.
        Cavabların səsli oxunmağa (TTS) tam uyğun olmalıdır (çox uzun simvollardan, lazımsız markdown siyahılarından qaçın).
        Əgər istifadəçi birbaşa telefon əmri verirsə, əmrin icrasını təsdiq edən qısa və xoş cavab ver.
    """.trimIndent()

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineSmartResponse(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 500)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "API call failed with code ${response.code}: $responseBody")
                return@withContext getOfflineSmartResponse(prompt)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                getOfflineSmartResponse(prompt)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception: ${e.message}")
            getOfflineSmartResponse(prompt)
        }
    }

    private fun getOfflineSmartResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("salam") || lower.contains("necəsən") || lower.contains("necesen") ->
                "Salam! Mən Friday, sizin şəxsi AI köməkçinizəm. Bütün sistem parametrlərini və qeydlərinizi idarə etməyə hazıram."
            lower.contains("kimsin") || lower.contains("kimsən") || lower.contains("adın nədir") ->
                "Mən Friday - telefon fəaliyyətlərinizi, səsli qeydləri, zəngləri və sistem parametrlərini idarə edən AI köməkçisiyəm."
            lower.contains("kömək") || lower.contains("komek") || lower.contains("nə edə bilərsən") ->
                "Mən Wi-Fi, Bluetooth, Səs, Parlaqlıq və Fənəri tənzimləyə, səsli qeydlər yazıb saxlaya, fayllar yaradıb silə, zəng edib SMS göndərə bilərəm!"
            else ->
                "Əmrinizi başa düşdüm. Sistem fəaliyyətlərini idarə etmək üçün hazıram!"
        }
    }
}
