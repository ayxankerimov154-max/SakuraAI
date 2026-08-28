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

    suspend fun askGemini(prompt: String, customApiKey: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = (customApiKey?.trim()?.ifBlank { null } ?: BuildConfig.GEMINI_API_KEY.trim())
        
        // Only call remote Gemini endpoint if a syntactically valid Google API key is provided
        val isValidKey = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "YOUR_GEMINI_API_KEY" &&
                apiKey.length >= 25 &&
                (apiKey.startsWith("AIza") || apiKey.length > 30)

        if (!isValidKey) {
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
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("salam") || lower.contains("sabahın xeyir") || lower.contains("hər vaxtınız xeyir") ->
                "Salam! Mən Friday, sizin xidmətinizdəyəm. Sizə necə kömək edə bilərəm?"
            lower.contains("necəsən") || lower.contains("necesen") || lower.contains("keyfin necədir") ->
                "Əlayəm, təşəkkür edirəm! Bütün sistemlər tam işlək vəziyyətdədir və əmrlərinizi icra etməyə hazıram."
            lower.contains("kimsin") || lower.contains("kimsən") || lower.contains("adın nədir") || lower.contains("adin nedir") ->
                "Mən Friday - telefon fəaliyyətlərinizi, səsli qeydləri, zəngləri və sistem parametrlərini idarə edən şəxsi AI köməkçinizəm."
            lower.contains("kömək") || lower.contains("komek") || lower.contains("nə edə bilərsən") || lower.contains("funksiyaların") ->
                "Mən Wi-Fi, Bluetooth, Səs, Parlaqlıq və Fənəri tənzimləyə, səsli qeydlər yazıb saxlaya, fayllar yaradıb redaktə edə, birbaşa zəng edib SMS göndərə bilərəm!"
            lower.contains("saat") || lower.contains("vaxt") -> {
                val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                "Hazırda saat $now-dır."
            }
            lower.contains("tarix") || lower.contains("bu gün") -> {
                val today = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                "Bu gün: $today."
            }
            lower.contains("təşəkkür") || lower.contains("cox sag ol") || lower.contains("çox sağ ol") || lower.contains("sağ ol") ->
                "Xoşdur! Hər zaman xidmətinizdəyəm."
            else ->
                "Əmrinizi başa düşdüm. Telefon parametrlərini və qeydlərinizi idarə etmək üçün hazıram!"
        }
    }
}
