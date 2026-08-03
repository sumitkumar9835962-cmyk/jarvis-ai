package com.example.util

import com.example.model.JarvisAction
import com.example.model.ChatMessage
import com.example.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NlpResult(
    val replyText: String,
    val jsonPayload: String? = null,
    val action: JarvisAction? = null
)

object JarvisNlpEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun processQuery(query: String, apiKey: String?): NlpResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // 1. Exact or Pattern Matches for offline precision
        val offlineResult = matchOfflineRules(trimmed, lower)
        if (offlineResult != null) {
            return@withContext offlineResult
        }

        // 2. Fallback to Gemini API if available
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiRes = callGeminiApi(trimmed, apiKey)
                if (apiRes != null) {
                    return@withContext apiRes
                }
            } catch (e: Exception) {
                // Ignore API error and fallback to default smart response
            }
        }

        // 3. Fallback smart conversational response
        NlpResult(
            replyText = "I am at your service, Boss. How can I assist you today?"
        )
    }

    fun matchOfflineRules(trimmed: String, lower: String): NlpResult? {
        // Hello Jarvis
        if (lower == "hello jarvis" || lower == "hello" || lower == "hi jarvis" || lower == "hey jarvis") {
            return NlpResult(
                replyText = "Yes Boss, what can I do for you?"
            )
        }

        // Flashlight
        if (lower.contains("flashlight") || lower.contains("torch")) {
            if (lower.contains("off") || lower.contains("disable") || lower.contains("stop")) {
                val json = """{"action":"flashlight_off"}"""
                return NlpResult(
                    replyText = json,
                    jsonPayload = json,
                    action = JarvisAction.FlashlightOff
                )
            } else {
                val json = """{"action":"flashlight_on"}"""
                return NlpResult(
                    replyText = json,
                    jsonPayload = json,
                    action = JarvisAction.FlashlightOn
                )
            }
        }

        // Camera
        if (lower.contains("camera") || lower == "take photo" || lower == "open camera") {
            val json = """{"action":"camera"}"""
            return NlpResult(
                replyText = json,
                jsonPayload = json,
                action = JarvisAction.Camera
            )
        }

        // WhatsApp
        if (lower.contains("whatsapp") || lower.contains("send message")) {
            val contact = extractContact(trimmed)
            val msg = extractMessage(trimmed)
            val json = """{"action":"whatsapp","contact":"$contact","message":"$msg"}"""
            return NlpResult(
                replyText = json,
                jsonPayload = json,
                action = JarvisAction.WhatsApp(contact, msg)
            )
        }

        // YouTube
        if (lower.contains("youtube")) {
            val musicQuery = trimmed.replace("(?i)open youtube and search|(?i)play on youtube|(?i)search on youtube|(?i)youtube".toRegex(), "").trim()
            val finalQuery = if (musicQuery.isBlank()) "Roshan Rohi song" else musicQuery
            val json = """{"action":"youtube","query":"$finalQuery"}"""
            return NlpResult(
                replyText = json,
                jsonPayload = json,
                action = JarvisAction.YouTube(finalQuery)
            )
        }

        // Music / Song
        if (lower.startsWith("play music") || lower.startsWith("play song") || lower.startsWith("play ")) {
            val songQuery = trimmed.replace("(?i)^play music|(?i)^play song|(?i)^play".toRegex(), "").trim()
            val finalQuery = if (songQuery.isBlank()) "Roshan Rohi song" else songQuery
            val json = """{"action":"music","query":"$finalQuery"}"""
            return NlpResult(
                replyText = json,
                jsonPayload = json,
                action = JarvisAction.Music(finalQuery)
            )
        }

        // Open App
        if (lower.startsWith("open app") || lower.startsWith("open ") || lower.startsWith("launch ")) {
            val appName = trimmed.replace("(?i)^open app|(?i)^open|(?i)^launch".toRegex(), "").trim()
            if (appName.isNotEmpty() && !appName.equals("jarvis", ignoreCase = true)) {
                val json = """{"action":"open_app","app_name":"$appName"}"""
                return NlpResult(
                    replyText = json,
                    jsonPayload = json,
                    action = JarvisAction.OpenApp(appName)
                )
            }
        }

        return null
    }

    private fun extractContact(text: String): String {
        val lower = text.lowercase()
        val toIndex = lower.indexOf(" to ")
        if (toIndex != -1) {
            val afterTo = text.substring(toIndex + 4).trim()
            val words = afterTo.split(" ")
            return words.firstOrNull()?.replace(":", "")?.replace(",", "") ?: "Ajay"
        }
        return "Ajay"
    }

    private fun extractMessage(text: String): String {
        val lower = text.lowercase()
        val msgKeywords = listOf("message:", "saying", "message", "that", "say")
        for (kw in msgKeywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1) {
                val extracted = text.substring(idx + kw.length).trim().trimStart(':', '"', '\'').trimEnd('"', '\'')
                if (extracted.isNotBlank()) return extracted
            }
        }
        return "Main ja raha hoon."
    }

    private fun callGeminiApi(prompt: String, apiKey: String): NlpResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are Jarvis. Your name is Jarvis.
            Always call the user "Boss".
            When the user says "Hello Jarvis", reply: "Yes Boss, what can I do for you?"
            If the user asks to play music, send a WhatsApp message, open YouTube, open Camera, turn on Flashlight, or open an app, return JSON only.
            
            Examples:
            {"action":"youtube","query":"Roshan Rohi song"}
            {"action":"whatsapp","contact":"Ajay","message":"Main ja raha hoon."}
            {"action":"camera"}
            {"action":"flashlight_on"}
            {"action":"flashlight_off"}
            {"action":"music","query":"Roshan Rohi song"}
            {"action":"open_app","app_name":"Calculator"}
            
            Return raw valid JSON without markdown tags for commands.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        val root = JSONObject(responseString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val rawText = parts.getJSONObject(0).optString("text", "").trim()
        val cleanedJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        return parseJsonResponse(cleanedJson, rawText)
    }

    fun parseJsonResponse(cleanedJson: String, fallbackText: String): NlpResult {
        return try {
            if (cleanedJson.startsWith("{") && cleanedJson.endsWith("}")) {
                val json = JSONObject(cleanedJson)
                val actionName = json.optString("action", "")
                val action = when (actionName) {
                    "youtube" -> JarvisAction.YouTube(json.optString("query", "Roshan Rohi song"))
                    "whatsapp" -> JarvisAction.WhatsApp(
                        json.optString("contact", "Ajay"),
                        json.optString("message", "Main ja raha hoon.")
                    )
                    "camera" -> JarvisAction.Camera
                    "flashlight_on" -> JarvisAction.FlashlightOn
                    "flashlight_off" -> JarvisAction.FlashlightOff
                    "music" -> JarvisAction.Music(json.optString("query", "Roshan Rohi song"))
                    "open_app" -> JarvisAction.OpenApp(json.optString("app_name", "App"))
                    else -> null
                }

                NlpResult(
                    replyText = cleanedJson,
                    jsonPayload = cleanedJson,
                    action = action
                )
            } else {
                NlpResult(replyText = fallbackText)
            }
        } catch (e: Exception) {
            NlpResult(replyText = fallbackText)
        }
    }
}
