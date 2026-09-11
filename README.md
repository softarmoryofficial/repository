# JARVIS Android

A modern Android voice assistant built with Kotlin, Jetpack Compose, Android SpeechRecognizer/TextToSpeech, and a Railway backend.

## What it does

- 🎙️ Voice input with Android SpeechRecognizer
- 🔊 Spoken responses with Android TextToSpeech
- 🧠 OpenRouter chat through a backend; provider keys stay off-device
- 🆓 Always routes inference through OpenRouter's `openrouter/free` model router
- 🔁 Up to 100 OpenRouter API keys with automatic failover on key/rate/server errors
- ⚡ Local commands such as opening supported websites
- 🛰️ Railway-ready Node backend
- 🔐 Backend-only provider secrets
- 🔑 Android opens the OpenRouter key page automatically when the backend has no configured key

## Architecture

```text
Android app
  ├─ Compose UI
  ├─ SpeechRecognizer / TTS
  ├─ Safe local intent router
  └─ HTTPS JSON client
          │
          ▼
     Railway backend
          ├─ OpenRouter key pool (1..100)
          ├─ cooldown + automatic failover
          └─ openrouter/free
                    │
                    ▼
             Free model selected by OpenRouter
```

## OpenRouter key pool

The backend accepts either one packed variable or numbered variables. The pool is capped at 100 unique keys.

### Option A — packed

```bash
OPENROUTER_API_KEYS="sk-or-v1-...\nsk-or-v1-...\nsk-or-v1-..."
```

Comma-separated values are also accepted.

### Option B — numbered

```bash
OPENROUTER_API_KEY_1=sk-or-v1-...
OPENROUTER_API_KEY_2=sk-or-v1-...
# ...
OPENROUTER_API_KEY_100=sk-or-v1-...
```

Never commit real keys. Put them in Railway Variables or another secrets manager. Railway supports adding variables through the service Variables tab or Raw Editor.

When a request succeeds, the backend advances the key pointer. On authentication, quota, timeout, rate-limit, or 5xx failures, the failing key is temporarily cooled down and the backend automatically tries another available key. If every configured key is unavailable, the API returns a setup/failure response instead of looping forever.

## Free-model guarantee

The backend hard-codes:

```text
openrouter/free
```

This is OpenRouter's free-model router. It selects from the currently available free models. Free-model availability and rate limits can change, so the backend treats rate-limit and provider failures as failover conditions.

## Run Android

Open the repository in Android Studio, let Gradle sync, then run the `app` configuration on an Android 8+ device/emulator.

The current production backend is:

```text
https://jarvis-backend-production-673e.up.railway.app
```

The app does not store any OpenRouter API key.

## Run backend locally

```bash
cd backend
npm install
OPENROUTER_API_KEY_1=sk-or-v1-... npm start
```

Or use the packed form:

```bash
OPENROUTER_API_KEYS="sk-or-v1-...,sk-or-v1-..." npm start
```

The backend exposes:

- `GET /health` — service/provider/key-pool status
- `GET /config` — non-secret configuration state
- `POST /chat` — JARVIS chat endpoint

## No-key behavior

When `/chat` reports `NO_OPENROUTER_KEY`, the Android app opens:

```text
https://openrouter.ai/settings/keys
```

It also includes a visible **Get OpenRouter API Key** button.

## Security

Do **not** put an OpenRouter key in the Android APK, Git repository, or public source. OpenRouter recommends protecting keys and using environment variables. The APK only talks to the JARVIS backend.

## Roadmap

- Streaming responses
- Room persistence and encrypted local history
- Tool/plugin registry with explicit user confirmation
- Optional on-device fallback
- Wear OS companion
