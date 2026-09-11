# JARVIS Android

A modern Android voice assistant starter built with Kotlin, Jetpack Compose, Android SpeechRecognizer/TextToSpeech, and a small secure backend.

## What it does

- 🎙️ Voice input with Android SpeechRecognizer
- 🔊 Spoken responses with Android TextToSpeech
- 🧠 LLM chat through a backend (API keys stay off-device)
- ⚡ Local commands: web search, maps, open settings, timer, flashlight
- 💾 Lightweight local conversation state
- 🛰️ Railway-ready Node backend
- 🔐 Backend-only provider secrets

## Architecture

```text
Android app
  ├─ Compose UI
  ├─ SpeechRecognizer / TTS
  ├─ Intent router for safe local commands
  └─ HTTPS JSON client
          │
          ▼
     Railway backend
          └─ OpenAI-compatible /chat endpoint
```

## Run Android

Open the repository in Android Studio, let Gradle sync, then run the `app` configuration on an Android 8+ device/emulator. Add the backend URL to `local.properties`:

```properties
JARVIS_BACKEND_URL=https://YOUR-RAILWAY-DOMAIN
```

## Run backend locally

```bash
cd backend
npm install
OPENAI_API_KEY=... OPENAI_MODEL=... npm start
```

The backend exposes `GET /health` and `POST /chat`.

## Security

Do **not** put an LLM provider API key in the Android APK. The backend is deliberately responsible for provider credentials. The Android app only knows the backend URL.

## Roadmap

- Streaming responses
- Wake-word mode using an appropriate on-device detector
- Room persistence and encrypted local history
- Tool/plugin registry with explicit user confirmation
- Optional on-device model fallback
- Wear OS companion
