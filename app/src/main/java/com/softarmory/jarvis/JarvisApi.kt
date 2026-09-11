package com.softarmory.jarvis

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterSetupRequired(val setupUrl: String) : IllegalStateException("OpenRouter API key is not configured")

object JarvisApi {
    const val BASE_URL = "https://jarvis-backend-production-673e.up.railway.app"
    const val OPENROUTER_KEYS_URL = "https://openrouter.ai/settings/keys"

    fun chat(userText: String): String {
        val connection = (URL("$BASE_URL/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", userText))
        val body = JSONObject().put("messages", messages).toString()
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (status !in 200..299) {
            val error = runCatching { JSONObject(response) }.getOrNull()
            if (error?.optString("code") == "NO_OPENROUTER_KEY") {
                throw OpenRouterSetupRequired(error.optString("setupUrl", OPENROUTER_KEYS_URL))
            }
            throw IllegalStateException(error?.optString("error", response) ?: response)
        }
        return JSONObject(response).optString("message", "No response")
    }
}
