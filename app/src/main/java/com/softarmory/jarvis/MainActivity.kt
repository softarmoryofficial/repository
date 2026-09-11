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
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent { JarvisScreen(::listen, ::speak, ::runLocalCommand) }
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO); return
        }
        startListening()
    }

    private fun startListening() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    handleCommand(text)
                }
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

    private fun handleCommand(text: String) {
        val command = text.lowercase(Locale.getDefault())
        if (command.contains("open youtube")) { runLocalCommand("youtube"); return }
        if (command.contains("open google")) { runLocalCommand("google"); return }
        speak("I heard: $text. Connect the backend to enable full AI reasoning.")
    }

    private fun runLocalCommand(target: String) {
        val url = when (target) { "youtube" -> "https://youtube.com"; else -> "https://google.com" }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }

    override fun onDestroy() { recognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}

@Composable
private fun JarvisScreen(onListen: () -> Unit, onSpeak: (String) -> Unit, onLocal: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf("JARVIS online.") }
    MaterialTheme {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JARVIS", style = MaterialTheme.typography.headlineLarge)
                Text("Android AI Assistant", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) { items(messages) { Text(it, Modifier.padding(8.dp)) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), label = { Text("Command") })
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (input.isNotBlank()) { messages += "You: $input"; onSpeak("I heard: $input"); input = "" } }) { Text("Send") }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onListen() }, modifier = Modifier.fillMaxWidth()) { Text("🎙 Speak to JARVIS") }
            }
        }
    }
}
