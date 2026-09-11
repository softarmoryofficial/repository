package com.softarmory.jarvis

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object JarvisApi {
    // Replace with your deployed Railway URL. Never put the provider API key here.
    const val BASE_URL = "https://YOUR-RAILWAY-DOMAIN"

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
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) throw IllegalStateException(response)
        return JSONObject(response).optString("message", "No response")
    }
}
