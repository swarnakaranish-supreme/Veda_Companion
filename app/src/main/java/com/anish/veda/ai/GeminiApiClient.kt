package com.anish.veda.ai

import com.anish.veda.data.repository.AiSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class GeminiApiResult {
    data class Success(val responseText: String, val smartReplies: List<String>) : GeminiApiResult()
    data class Error(val errorMessage: String) : GeminiApiResult()
}

class GeminiApiClient(
    private val settingsRepository: AiSettingsRepository
) {

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): GeminiApiResult = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.apiKey.value.trim()
        if (apiKey.isEmpty()) {
            return@withContext GeminiApiResult.Error("Gemini API key is not configured. Please set your key in AI Settings.")
        }

        if (!settingsRepository.dataSharingConsent.value) {
            return@withContext GeminiApiResult.Error("External AI consent is disabled. Enable Data Sharing Consent in AI Settings to use cloud AI.")
        }

        val model = settingsRepository.modelName.value
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000

            // Construct payload
            val root = JSONObject()

            // System Instructions for VEDA persona
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject().put("text", SYSTEM_PROMPT)
            sysParts.put(sysPart)
            systemInstruction.put("parts", sysParts)
            root.put("system_instruction", systemInstruction)

            // Contents Array (history + current prompt)
            val contentsArray = JSONArray()

            // Add up to 6 recent history turns
            for ((role, text) in conversationHistory.takeLast(6)) {
                val turn = JSONObject()
                turn.put("role", if (role.lowercase() == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                turn.put("parts", parts)
                contentsArray.put(turn)
            }

            // Current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", prompt))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            root.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("maxOutputTokens", 512)
            root.put("generationConfig", genConfig)

            // Send request
            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseGeminiResponse(responseStr)
                GeminiApiResult.Success(parsed.first, parsed.second)
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val errorMsg = try {
                    val errJson = JSONObject(errorStream)
                    errJson.getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "HTTP $responseCode: $errorStream"
                }
                GeminiApiResult.Error("Gemini Error: $errorMsg")
            }
        } catch (e: Exception) {
            GeminiApiResult.Error("Network connection failed: ${e.message}")
        }
    }

    private fun parseGeminiResponse(jsonStr: String): Pair<String, List<String>> {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates") ?: return "No response generated" to emptyList()
        if (candidates.length() == 0) return "Empty response" to emptyList()

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        // Generate smart reply suggestions based on output
        val smartReplies = mutableListOf<String>()
        if (text.contains("?")) {
            smartReplies.add("Yes, proceed")
            smartReplies.add("Not right now")
        } else {
            smartReplies.add("Tell me more")
            smartReplies.add("Check health status")
        }

        return text.trim() to smartReplies
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You are VEDA AI, a tactical, intelligent, and concise AI wearable companion designed for the VEDA Smartwatch. " +
            "Your personality is futuristic, precise, and supportive. " +
            "Keep responses succinct (under 3 sentences when possible) for wearable displays and voice readout. " +
            "Do not give medical diagnosis; include a health disclaimer if health is mentioned. " +
            "Never generate fake health readings."
    }
}
