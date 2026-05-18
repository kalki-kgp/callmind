# CallMind

AI-powered Android app that turns phone call recordings into searchable, summarized conversation memory.

CallMind does **not** record calls. It watches a folder on your device (or imports files manually), transcribes audio, runs LLM analysis, and stores everything locally so you can browse, search, and export summaries.

## Features

- **Automatic discovery** — foreground service monitors your recording directory via MediaStore and can auto-process new files
- **Manual import** — pick recordings from anywhere with the system file picker
- **Transcription** — cloud STT via [Google Gemini](https://ai.google.dev/) multimodal, or on-device [Vosk](https://alphacephei.com/vosk/) (36 MB model download)
- **Call analysis** — summaries, key points, topics, sentiment, and checkable action items
- **Semantic search** — Gemini `text-embedding-004` vectors stored in Room with local cosine similarity
- **Text search** — keyword search across transcripts and summaries
- **Per-contact history** — conversation threads grouped by contact
- **Export** — share call summaries as text files
- **Flexible LLM backend** — Gemini Flash for analysis, or any OpenAI-compatible API (base URL, key, model)

## How it works

```
New recording detected
        │
        ▼
┌───────────────────┐
│ TranscriptionWorker│  Gemini (cloud) or Vosk (on-device)
└─────────┬─────────┘
          ▼
┌───────────────────┐
│  AnalysisWorker   │  Gemini or OpenAI-compatible LLM
└─────────┬─────────┘
          ▼
┌───────────────────┐
│  EmbeddingWorker  │  Gemini text-embedding-004
└─────────┬─────────┘
          ▼
   Room database + notifications
```

Recordings are matched to contacts using filename parsing and the Android call log. When matching fails, metadata is inferred from the filename alone.

## Requirements

| | |
|---|---|
| **Android** | API 29+ (Android 10+) |
| **JDK** | 21 recommended (Gradle can auto-provision via Foojay) |
| **Android SDK** | API 36.1 (`platforms;android-36.1`) for current AGP 9.1 |
| **Device** | Phone with existing call recordings (e.g. OEM dialer saves to `Music/Recordings/Call Recordings/`) |
| **API keys** | [Gemini API key](https://aistudio.google.com/apikey) required for cloud transcription and embeddings; optional second key/endpoint if using OpenAI-compatible analysis |

## Getting started

### 1. Clone and configure SDK

```bash
git clone <repo-url> CallMind
cd CallMind
```

Create `local.properties` in the project root (gitignored):

```properties
sdk.dir=/path/to/your/Android/sdk
```

### 2. Build

```bash
./gradlew assembleDebug
```

Install the APK from `app/build/outputs/apk/debug/` on your device, or run from Android Studio.

### 3. First launch

1. Grant **audio**, **call log**, **contacts**, and **notifications** permissions.
2. Open **Settings** and set:
   - **Recording directory** — default: `Music/Recordings/Call Recordings` (adjust for your OEM)
   - **Gemini API key** — required for cloud STT and semantic search
   - **Analysis LLM** — Gemini (default) or OpenAI-compatible provider
   - **Speech-to-Text** — enable local Vosk and download the model if you want offline transcription
   - **Auto-process** — optionally process new recordings automatically
3. Use **Scan** on the home screen to index existing recordings, or enable auto-process and wait for new files.

## Configuration reference

| Setting | Purpose |
|---------|---------|
| Recording directory | Relative path under shared storage where your dialer saves WAV/other audio |
| Gemini API key | Cloud transcription, embeddings, and Gemini-based analysis |
| Analysis LLM provider | `Gemini` or `OpenAI Compatible` (custom base URL, key, model) |
| Use local STT (Vosk) | On-device transcription; no Gemini needed for STT when enabled |
| Auto-process | Run the full pipeline when new recordings appear |

Connection test buttons in Settings help verify API keys before processing a backlog.

## Permissions

| Permission | Why |
|------------|-----|
| `READ_MEDIA_AUDIO` | Read call recording files |
| `READ_CALL_LOG` | Match recordings to phone numbers and call times |
| `READ_CONTACTS` | Resolve contact names |
| `POST_NOTIFICATIONS` | Processing progress and completion alerts |
| `FOREGROUND_SERVICE` | Background monitoring and WorkManager jobs |
| `INTERNET` | Cloud STT, LLM, and embedding APIs |

## Development

### Project structure

```
app/src/main/java/com/callmind/app/
├── CallMindApplication.kt       # Hilt + WorkManager
├── MainActivity.kt
├── di/                          # Hilt modules
├── data/
│   ├── local/db/                # Room entities & DAOs
│   ├── local/preferences/       # DataStore settings
│   ├── local/                   # Vosk STT
│   ├── remote/                  # Gemini & OpenAI-compatible APIs
│   └── repository/
├── service/
│   ├── RecordingMonitorService  # ContentObserver + backlog scan
│   ├── PipelineOrchestrator     # WorkManager chain
│   └── worker/                  # Transcription, Analysis, Embedding
└── ui/                          # Compose screens (home, detail, search, contact, settings)
```

### Common commands

| Task | Command |
|------|---------|
| Debug APK | `./gradlew assembleDebug` |
| Release APK | `./gradlew assembleRelease` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lint` |
| Full check | `./gradlew check` |

### Tech stack

- **Kotlin** · **Jetpack Compose** · **Material 3**
- **MVVM** · **Hilt** · **Coroutines / Flow**
- **Room** · **DataStore** · **WorkManager**
- **Retrofit** · **OkHttp** · **Kotlin Serialization**
- **Vosk** (local STT)

### SDK note

This project uses AGP 9.1 with `compileSdk 36.1`. Install the matching platform in the SDK Manager:

```bash
sdkmanager "platforms;android-36.1"
```

## Privacy & data

- Recordings stay on your device; only audio/text you choose to process is sent to configured cloud APIs.
- API keys are stored in app-private DataStore preferences.
- This is a personal / open-source project — not designed or audited for Play Store distribution.

## Roadmap

- [ ] On-device STT improvements (whisper.cpp integration)
- [ ] Pull-to-refresh and richer error/retry UI on the home screen
- [ ] Optional dark/light theme toggle (currently follows system)

## License

No license file is included yet. Add one before public distribution if you plan to open-source the repo.
