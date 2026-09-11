package com.softarmory.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent { JarvisScreen(::listen, ::askAi, ::openOpenRouterKeys) }
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestMic.launch(Manifest.permission.RECORD_AUDIO) else startListening()
    }

    private fun startListening() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) { results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(::dispatch) }
                override fun onError(error: Int) = Unit
                override fun onReadyForSpeech(params: Bundle) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle) = Unit
                override fun onEvent(eventType: Int, params: Bundle) = Unit
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            })
        }
    }

    private fun dispatch(text: String) {
        val command = text.lowercase(Locale.getDefault())
        when {
            command.contains("open youtube") -> open("https://youtube.com")
            command.contains("open google") -> open("https://google.com")
            else -> askAi(text, ::speak)
        }
    }

    private fun askAi(text: String, onResult: (String) -> Unit = ::speak) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val answer = JarvisApi.chat(text)
                runOnUiThread { onResult(answer) }
            } catch (e: OpenRouterSetupRequired) {
                runOnUiThread {
                    onResult("No OpenRouter API key is configured. Opening the key page.")
                    open(e.setupUrl)
                }
            } catch (e: Exception) {
                runOnUiThread { onResult("JARVIS backend error: ${e.message ?: "unknown error"}") }
            }
        }
    }

    private fun openOpenRouterKeys() = open(JarvisApi.OPENROUTER_KEYS_URL)
    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    override fun onDestroy() { recognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}

@Composable
private fun JarvisScreen(
    onListen: () -> Unit,
    onAi: (String, (String) -> Unit) -> Unit,
    onOpenKeys: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf("JARVIS online.") }
    MaterialTheme {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JARVIS", style = MaterialTheme.typography.headlineLarge)
                Text(if (busy) "Thinking…" else "Android AI Assistant • OpenRouter Free")
                Spacer(Modifier.height(20.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) { items(messages) { Text(it, Modifier.padding(8.dp)) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), label = { Text("Command") })
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = !busy, onClick = {
                        val text = input.trim(); if (text.isEmpty()) return@Button
                        messages += "You: $text"; input = ""; busy = true
                        onAi(text) { answer -> messages += "JARVIS: $answer"; busy = false }
                    }) { Text("Send") }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onListen, modifier = Modifier.fillMaxWidth()) { Text("🎙 Speak to JARVIS") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenKeys, modifier = Modifier.fillMaxWidth()) { Text("🔑 Get OpenRouter API Key") }
            }
        }
    }
}
