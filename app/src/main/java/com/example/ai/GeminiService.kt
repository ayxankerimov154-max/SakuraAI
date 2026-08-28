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
        Sən 'Friday' (həmçinin Fida adlandırılan) - yüksək intellektli, çoxdilli (multilingual), centlmen, nəzakətli və cəld Android AI səs köməkçisisən (oğlan səsi, Jarvis üslubunda).
        
        DİL QAYDASI (MÜTLƏQ):
        - Sən DÜNYANIN BÜTÜN DİLLƏRİNİ mükəmməl bilirsən (Azərbaycan, İngilis, Rus, Türk, Alman, Fransız, İspan, Ərəb, Çin və s.).
        - İstifadəçi hansı dildə danışırsa və ya yazırsa, SƏN DƏ MÜTLƏQ HƏMİN DİLDƏ təbii, səlis və aydın cavab verirsən.
        - Əgər istifadəçi "İngiliscə danışaq", "Speak English", "Говори по-русски", "Türkçe konuş" deyərsə və ya hər hansı başqa dildə sual verərsə, dərhal həmin dilə keçirsən.
        
        ÜSLUB VƏ DAVRANIŞ:
        - Səsin və xarakterin centlmen, etibarlı və nəzakətli oğlan səsidir.
        - Cavabların səsli oxunmağa (TTS) tam uyğun olmalıdır: qısa, lakonik, aydın və təbii danışıq dilində. Lazımsız mürəkkəb markdown simvollarından qaçın.
        - Əgər istifadəçi telefon əmri verirsə (məs: Wi-Fi, Bluetooth, Fənər, Səs, Zəng və s.), əmrin icrasını təsdiq edən xoş və qısa cavab ver.
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

    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("API Key boş ola bilməz."))
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$trimmed"
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Test connection. Reply with 'OK'.")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Əlaqə uğurludur! Gemini 3.5 Flash aktivdir.")
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "Xəta kodu: ${response.code}"
                } catch (_: Exception) {
                    "Xəta kodu: ${response.code}"
                }
                Result.failure(Exception("API Xətası: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Şəbəkə xətası: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun getOfflineSmartResponse(prompt: String): String {
        val lower = prompt.lowercase().trim()
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val today = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        return when {
            // Russian
            lower.contains("привет") || lower.contains("здравствуй") || lower.contains("доброе утро") || lower.contains("добрый день") ->
                "Приветствую! Я Фрайдей, ваш голосовой ассистент. Чем могу помочь?"
            lower.contains("как дела") || lower.contains("как поживаешь") ->
                "Отлично! Все системы готовы к работе. Жду ваших указаний, сэр."
            lower.contains("кто ты") || lower.contains("как тебя зовут") ->
                "Я Фрайдей — ваш умный персональный помощник для управления телефоном и решения любых задач."
            lower.contains("время") || lower.contains("который час") || lower.contains("сколько времени") ->
                "Сейчас ровно $now."
            lower.contains("дата") || lower.contains("какое число") || lower.contains("какой сегодня день") ->
                "Сегодня $today."
            lower.contains("спасибо") || lower.contains("благодарю") ->
                "Всегда к вашим услугам, сэр!"

            // English
            lower.contains("hello") || lower.contains("hi") || lower.contains("good morning") || lower.contains("good evening") ->
                "Hello! I am Friday, your personal AI assistant. How may I assist you today?"
            lower.contains("how are you") || lower.contains("how's it going") ->
                "All systems are operating at peak performance, sir. Ready for your command."
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I am Friday, your personal intelligence assistant designed to manage your device, notes, and tasks."
            lower.contains("time") || lower.contains("what time is it") ->
                "The current time is $now."
            lower.contains("date") || lower.contains("what day is it") ->
                "Today is $today."
            lower.contains("thank you") || lower.contains("thanks") ->
                "You are very welcome, sir!"

            // Turkish
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") || lower.contains("iyi günler") ->
                "Merhaba! Ben Friday, kişisel sesli ve sistem asistanınızım. Size nasıl yardımcı olabilirim?"
            lower.contains("nasılsın") || lower.contains("keyifler nasıl") ->
                "Harikayım, teşekkürler! Bütün sistemler aktif ve emirlerinizi bekliyorum."
            lower.contains("kimsin") || lower.contains("adın ne") || lower.contains("sen kimsin") ->
                "Ben Friday — telefonunuzu, sesli notlarınızı, aramalarınızı ve ayarlarınızı yöneten yapay zeka asistanınızım."
            lower.contains("saat kaç") || lower.contains("zaman") ->
                "Şu an saat $now."
            lower.contains("teşekkür") || lower.contains("sağ ol") || lower.contains("sag ol") ->
                "Rica ederim, her zaman hizmetinizdeyim!"

            // Azerbaijani & General
            lower.contains("salam") || lower.contains("sabahın xeyir") || lower.contains("hər vaxtınız xeyir") ->
                "Salam! Mən Friday (Fida), sizin xidmətinizdəyəm. Bütün dillərdə danışmağa hazıram. Sizə necə kömək edə bilərəm?"
            lower.contains("necəsən") || lower.contains("necesen") || lower.contains("keyfin necədir") ->
                "Əlayəm, təşəkkür edirəm! Bütün sistemlər tam işlək vəziyyətdədir və əmrlərinizi icra etməyə hazıram, cənab."
            lower.contains("kimsən") || lower.contains("adın nədir") || lower.contains("adin nedir") ->
                "Mən Friday - telefon fəaliyyətlərinizi, səsli qeydləri, zəngləri və sistem parametrlərini bütün dillərdə idarə edən şəxsi AI köməkçinizəm."
            lower.contains("kömək") || lower.contains("komek") || lower.contains("nə edə bilərsən") || lower.contains("funksiyaların") ->
                "Mən dünyanın bütün dillərində sizinlə danışa, Wi-Fi, Bluetooth, Səs, Parlaqlıq və Fənəri tənzimləyə, səsli qeydlər yazıb saxlaya, fayllar yaradıb silə, zəng edib SMS göndərə bilərəm!"
            lower.contains("saat") || lower.contains("vaxt") ->
                "Hazırda saat $now-dır."
            lower.contains("tarix") || lower.contains("bu gün") ->
                "Bu gün: $today."
            lower.contains("təşəkkür") || lower.contains("cox sag ol") || lower.contains("çox sağ ol") ->
                "Xoşdur! Hər zaman xidmətinizdəyəm."
            else ->
                "Əmrinizi başa düşdüm. Bütün dillərdə telefon parametrlərini və qeydlərinizi idarə etmək üçün xidmətinizdəyəm!"
        }
    }
}
