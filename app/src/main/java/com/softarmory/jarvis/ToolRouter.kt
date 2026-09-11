package com.softarmory.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Small deterministic tool layer. Add new tools here instead of letting the LLM directly execute intents. */
object ToolRouter {
    fun tryExecute(context: Context, text: String): Boolean {
        val command = text.lowercase()
        val url = when {
            "open youtube" in command -> "https://youtube.com"
            "open google" in command -> "https://google.com"
            "open github" in command -> "https://github.com"
            else -> null
        } ?: return false
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return true
    }
}
