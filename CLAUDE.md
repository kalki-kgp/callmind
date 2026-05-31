# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# CallMind

AI-powered Android app that transforms phone call recordings into searchable, summarized conversation memory.

## Build Commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Run lint | `./gradlew lint` |
| Full check | `./gradlew check` |

Instrumented/UI tests (`connectedDebugAndroidTest`) require a connected device and cannot run in CI. Only `testDebugUnitTest` and lint are reliably executable without a device.

**AGP note**: The project uses AGP 9.1.0 with a non-standard `compileSdk` block syntax:
```kotlin
compileSdk {
    version = release(36) { minorApiLevel = 1 }
}
```
This requires `platforms;android-36.1` in the SDK. Do not simplify this to `compileSdk = 36`.

## Tech Stack

- **Language**: Kotlin, minSdk 29, targetSdk 36
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Hilt DI + Coroutines/Flow
- **Database**: Room v3
- **Background**: WorkManager (transcription/analysis/embedding jobs)
- **Monitoring**: MediaStore ContentObserver in Foreground Service
- **Local STT**: Vosk (vosk-android AAR + JNA) — model downloaded at runtime via `VoskModelManager`
- **Cloud STT**: Gemini 2.0 Flash multimodal (audio inlined as base64)
- **LLM**: Google Gemini 2.0 Flash **or** any OpenAI-compatible endpoint (configurable in Settings)
- **Embeddings**: Gemini `text-embedding-004` batch API
- **Search**: Text search (SQL LIKE) + semantic search (brute-force cosine similarity over stored embeddings)
- **Preferences**: DataStore

## Architecture

### Processing Pipeline

New recordings flow through a WorkManager chain: `TranscriptionWorker → AnalysisWorker → EmbeddingWorker`. All three workers are chained by `PipelineOrchestrator` under a unique work name `pipeline_<callId>` (REPLACE policy, so re-triggering retries). Each worker uses exponential back-off and retries up to 3 times; config errors (missing API key, model not downloaded) short-circuit to `Result.failure()` immediately and store the error message in `calls.processingError`.

### STT Mode Selection

`TranscriptionWorker` reads `UserPreferences.useLocalStt` at runtime to choose between:
- **Vosk** (`VoskTranscriptionService`): requires model downloaded via `VoskModelManager`; converts audio to 16 kHz mono PCM via `AudioConverter` before recognition
- **Gemini** (`GeminiTranscriptionService`): inlines the raw audio file as base64 in a multimodal request

### LLM Provider Selection

`AnalysisWorker` reads `UserPreferences.llmProvider` (`"gemini"` or `"openai_compatible"`) to choose between `GeminiApiService` and `OpenAiCompatibleService`. The OpenAI-compatible path uses a configurable base URL, API key, and model name (defaults to a Nebius-hosted DeepSeek endpoint).

### Database

`CallMindDatabase` is at **version 3**. When adding new columns/tables:
- Write an explicit `Migration` object and register it in `AppModule.provideDatabase`
- `MIGRATION_2_3` adds `calls.processingError TEXT DEFAULT NULL` — use it as a template
- `fallbackToDestructiveMigration(false)` is set, so missing migrations will crash rather than silently wipe data

### Key DataStore Preferences

| Key | Default | Purpose |
|---|---|---|
| `recording_directory` | `Music/Recordings/Call Recordings` | Root path scanned by ContentObserver |
| `gemini_api_key` | null | Required for cloud STT + Gemini LLM + embeddings |
| `use_local_stt` | false | Toggle Vosk vs Gemini for transcription |
| `llm_provider` | `"gemini"` | `"gemini"` or `"openai_compatible"` |
| `openai_base_url` / `openai_api_key` / `openai_model` | Nebius/DeepSeek defaults | OpenAI-compatible LLM config |
| `auto_process` | false | Auto-trigger pipeline on new recordings |

### Navigation

Single-activity app (`MainActivity`) with Compose Navigation. `NavGraph` gates on permissions: the permission screen is shown first if required permissions are missing. Routes: `home`, `call_detail/{callId}`, `contact/{phoneNumber}`, `search`, `settings`.

## Project Structure

```
com.callmind.app/
├── di/                         # Hilt modules (AppModule: Room/DAOs, NetworkModule: OkHttp/Retrofit)
├── data/
│   ├── local/
│   │   ├── db/                 # Room database, DAOs, entities (calls, transcripts, analysis, action_items, embeddings)
│   │   ├── preferences/        # UserPreferences (DataStore)
│   │   ├── VoskTranscriptionService.kt
│   │   └── VoskModelManager.kt
│   ├── remote/
│   │   ├── GeminiApiService.kt          # LLM analysis via Retrofit
│   │   ├── GeminiTranscriptionService.kt # Cloud STT (raw OkHttp, not Retrofit — audio as base64)
│   │   ├── GeminiEmbeddingService.kt
│   │   ├── OpenAiCompatibleService.kt   # OpenAI-compatible LLM fallback
│   │   └── model/              # AnalysisResult, GeminiModels
│   └── repository/             # CallRepository (single source of truth for all DB + file ops)
├── service/
│   ├── PipelineOrchestrator.kt  # Chains WorkManager jobs
│   ├── RecordingMonitorService  # Foreground service + ContentObserver
│   └── worker/                  # TranscriptionWorker, AnalysisWorker, EmbeddingWorker
├── ui/                          # MVVM screens: home, calldetail, contact, search, settings, permissions
└── util/
    ├── RecordingFileParser.kt   # Filename regex + call log matching
    ├── AudioConverter.kt        # WAV/other → 16 kHz mono PCM for Vosk
    ├── SemanticSearchEngine.kt  # Cosine similarity over EmbeddingEntity vectors
    ├── ExportHelper.kt          # Shareable text export via FileProvider
    └── NotificationHelper.kt   # Notification channel setup
```

## Device Target

OnePlus Nord 5 (Snapdragon 7+ Gen 3). Recording path default: `Music/Recordings/Call Recordings/`. Filenames contain contact name + phone number + datetime and are parsed by `RecordingFileParser`.
