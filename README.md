# CallMind

AI-powered Android app that turns phone call recordings into searchable, summarized conversation memory.

CallMind does **not** record calls. It watches a folder on your device (or imports files manually), transcribes audio, runs LLM analysis, and stores everything locally so you can browse, search, and export summaries.

## Features

- **Automatic discovery** — foreground service monitors your recording directory via MediaStore and can auto-process new files
- **Manual import** — pick recordings from anywhere with the system file picker
- **Transcription** — cloud STT via [Google Gemini](https://ai.google.dev/) multimodal (`gemini-flash-lite-latest`), or on-device [Vosk](https://alphacephei.com/vosk/) (36 MB model download)
- **Call analysis** — summaries, key points, topics, sentiment, and checkable action items
- **Semantic search** — cloud (`gemini-embedding-001`) or on-device (MediaPipe Universal Sentence Encoder) vectors in Room, with cosine similarity and model-matched querying
- **Text search** — keyword search across transcripts and summaries
- **Per-contact history** — conversation threads grouped by contact
- **Export** — share call summaries as text files
- **Flexible backends** — Gemini or OpenAI-compatible LLM for analysis; cloud or local embeddings (switching providers triggers a full re-index)

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
│  EmbeddingWorker  │  Gemini or on-device embedder (user setting)
└─────────┬─────────┘
          ▼
   Room database + notifications
```

`PipelineOrchestrator` chains the three workers under a unique WorkManager name per call (`pipeline_<callId>`, REPLACE policy). Config errors (missing API key, model not downloaded) fail fast and are stored on the call row as `processingError`.

Recordings are matched to contacts using filename parsing and the Android call log. When matching fails, metadata is inferred from the filename alone.

## Requirements

| | |
|---|---|
| **Android** | API 29+ (Android 10+) |
| **JDK** | 21 recommended (Gradle can auto-provision via Foojay) |
| **Android SDK** | API 36.1 (`platforms;android-36.1`) for current AGP 9.1 |
| **Device** | Phone with existing call recordings (e.g. OEM dialer saves to `Music/Recordings/Call Recordings/`) |
| **API keys** | [Gemini API key](https://aistudio.google.com/apikey) required for cloud transcription and cloud embeddings; optional second key/endpoint if using OpenAI-compatible analysis |

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
   - **Gemini API key** — required for cloud STT and cloud embeddings (and Gemini-based analysis)
   - **Analysis LLM** — Gemini (default) or OpenAI-compatible provider
   - **Speech-to-Text** — cloud (default) or local Vosk (download the model for offline STT)
   - **Search embeddings** — cloud Gemini or on-device model (download required for local)
   - **Auto-process** — optionally process new recordings automatically
3. Use **Scan** on the home screen to index existing recordings, or enable auto-process and wait for new files.

## Configuration reference

| Setting | Purpose |
|---------|---------|
| Recording directory | Relative path under shared storage where your dialer saves WAV/other audio |
| Gemini API key | Cloud transcription, cloud embeddings, and Gemini-based analysis |
| Analysis LLM provider | `Gemini` or `OpenAI Compatible` (custom base URL, key, model) |
| Use local STT (Vosk) | On-device transcription; no Gemini needed for STT when enabled |
| Search embeddings | `cloud` (`gemini-embedding-001`) or `local` (MediaPipe on-device model) |
| Auto-process | Run the full pipeline when new recordings appear |

Connection test buttons in Settings verify API keys and embedding providers before processing a backlog. Changing the embedding provider wipes the vector index and re-embeds analyzed calls so search only compares vectors from the same model.

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
├── di/                          # Hilt modules (Room, network)
├── data/
│   ├── local/db/                # Room v3 — calls, transcripts, analysis, embeddings
│   ├── local/preferences/       # DataStore settings
│   ├── local/                   # Vosk STT, embedding model download
│   ├── remote/                  # Gemini & OpenAI-compatible APIs, embedding providers
│   └── repository/              # CallRepository (single source of truth)
├── service/
│   ├── RecordingMonitorService  # ContentObserver + backlog scan
│   ├── PipelineOrchestrator     # WorkManager chain
│   └── worker/                  # Transcription, Analysis, Embedding
├── ui/                          # Compose — home, detail, search, contact, settings
└── util/                        # Recording parser, audio convert, semantic search, export
```

### Common commands

| Task | Command |
|------|---------|
| Debug APK | `./gradlew assembleDebug` |
| Release APK | `./gradlew assembleRelease` |
| Unit tests | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lint` |
| Full check | `./gradlew check` |

Instrumented tests (`connectedDebugAndroidTest`) need a connected device; CI typically runs unit tests and lint only.

### Tech stack

- **Kotlin** · **Jetpack Compose** · **Material 3**
- **MVVM** · **Hilt** · **Coroutines / Flow**
- **Room** · **DataStore** · **WorkManager**
- **Retrofit** · **OkHttp** · **Kotlin Serialization**
- **Vosk** (local STT) · **MediaPipe** (local embeddings)

### SDK note

This project uses AGP 9.1 with `compileSdk 36.1`. Install the matching platform in the SDK Manager:

```bash
sdkmanager "platforms;android-36.1"
```

Do not simplify the `compileSdk` block to `compileSdk = 36` — the minor API level is required.

### Codebase map (graphify)

A [graphify](https://github.com/safishamsi/graphify) knowledge graph can be generated locally for architecture exploration:

```bash
graphify extract .   # or full pipeline via /graphify in Claude Code
graphify query "How does CallRepository connect the pipeline?"
```

Outputs land in `graphify-out/` (gitignored). See `CLAUDE.md` for agent-oriented build and architecture notes.

## Privacy & data

- Recordings stay on your device; only audio/text you choose to process is sent to configured cloud APIs.
- API keys are stored in app-private DataStore preferences.
- Local embeddings run fully on-device after the model is downloaded.
- This is a personal / open-source project — not designed or audited for Play Store distribution.

## Roadmap

Planned native upgrades (Apex whisper.cpp STT, EmbeddingGemma on-device search) are documented in [`tobeimplemented.md`](tobeimplemented.md).

Near-term UI and quality work:

- [ ] Pull-to-refresh and richer error/retry UI on the home screen
- [ ] Optional dark/light theme toggle (currently follows system)
- [ ] Replace Vosk with Apex once whisper.cpp integration lands (see `tobeimplemented.md`)

## License

No license file is included yet. Add one before public distribution if you plan to open-source the repo.
