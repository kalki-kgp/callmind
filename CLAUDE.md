# CallMind

AI-powered Android app that transforms phone call recordings into searchable, summarized conversation memory.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Hilt DI + Coroutines/Flow
- **Database**: Room
- **Background**: WorkManager (transcription/analysis jobs)
- **Monitoring**: MediaStore ContentObserver in Foreground Service
- **Local STT**: whisper.cpp (base model, quantized) — TODO
- **Cloud STT fallback**: OpenAI Whisper API / Deepgram — TODO
- **LLM**: Google Gemini 2.0 Flash (via Retrofit)
- **Search**: Text search now, semantic search (ONNX + MiniLM) later
- **Preferences**: DataStore

## Project Structure
```
com.callmind.app/
├── CallMindApplication.kt      # Hilt app + WorkManager config
├── MainActivity.kt             # Single activity, Compose nav
├── di/                         # Hilt modules (AppModule, NetworkModule)
├── data/
│   ├── local/db/               # Room database, DAOs, entities
│   ├── local/preferences/      # DataStore user settings
│   ├── remote/                 # Gemini API service + models
│   └── repository/             # CallRepository (single source of truth)
├── service/
│   ├── RecordingMonitorService  # Foreground service, ContentObserver
│   └── worker/                  # TranscriptionWorker, AnalysisWorker
├── ui/
│   ├── navigation/             # NavGraph + Routes
│   ├── home/                   # Call list with summaries
│   ├── calldetail/             # Full call details + transcript
│   ├── search/                 # Text/semantic search
│   ├── contact/                # Per-contact conversation history
│   ├── settings/               # Config: directory, API key, STT mode
│   └── theme/                  # CallMindTheme, colors, typography
└── util/
    └── RecordingFileParser.kt  # Filename parsing + call log matching
```

## Device Info
- Target device: OnePlus Nord 5 (Snapdragon 7+ Gen 3)
- Recording path: `Music/Recordings/Call Recordings/`
- Recording format: typically WAV, filenames contain contact name + phone number + datetime

## Implementation Plan

### Phase 1 — Foundation (DONE)
- [x] Project setup (Compose, Material 3, Kotlin DSL)
- [x] Dependencies (Room, Hilt, WorkManager, Navigation, Retrofit, DataStore)
- [x] Room database schema (calls, transcripts, analysis, action_items)
- [x] DAOs with Flow-based queries
- [x] Repository layer
- [x] Hilt DI modules (AppModule, NetworkModule)
- [x] Gemini API service + request/response models
- [x] DataStore preferences (recording dir, API key, STT mode)
- [x] Navigation graph (home, call detail, contact, search, settings)
- [x] All screens scaffolded with ViewModels
- [x] RecordingMonitorService (ContentObserver)
- [x] TranscriptionWorker + AnalysisWorker stubs
- [x] RecordingFileParser (filename regex + call log matching)

### Phase 2 — Core Pipeline (DONE)
- [x] GeminiTranscriptionService — cloud STT using Gemini multimodal (audio → text)
- [x] AnalysisResult parser — handles JSON extraction, markdown stripping, fallback
- [x] TranscriptionWorker wired to GeminiTranscriptionService
- [x] AnalysisWorker wired with proper JSON parsing + action item extraction
- [x] PipelineOrchestrator — chains transcription → analysis via WorkManager
- [x] RecordingMonitorService triggers pipeline on new recordings + processes backlog
- [x] Duplicate detection (isRecordingProcessed) to avoid re-processing
- [x] NotificationHelper for all worker notification channels
- [x] HomeScreen with scan button, empty state, processing indicators, date/duration display
- [x] Fallback: createCallFromFilename when call log matching fails
- [ ] Integrate whisper.cpp via NDK/JNI for local STT (deferred to Phase 4)

### Phase 3 — Polish & Features (DONE)
- [x] Permission request flow with PermissionScreen (runtime permission UI)
- [x] Navigation gates on permissions before showing home
- [x] Proper date/time formatting across all screens
- [x] Call duration display in home + detail screens
- [x] Sentiment chips + topic chips in CallDetailScreen
- [x] Checkable action items with real toggle in CallDetailScreen
- [x] Key points section in CallDetailScreen
- [x] Empty state for HomeScreen
- [x] Scan button in HomeScreen toolbar
- [x] Polished SettingsScreen with card sections + descriptions
- [x] ContactScreen with topic chips + formatted call history
- [ ] Pull-to-refresh on home screen (nice-to-have)
- [ ] Error handling + retry UI (nice-to-have)

### Phase 4 — Semantic Search (DONE)
- [x] EmbeddingEntity + EmbeddingDao for vector storage in Room
- [x] GeminiEmbeddingService using text-embedding-004 batch API
- [x] SemanticSearchEngine with brute-force cosine similarity
- [x] EmbeddingWorker in pipeline: transcription → analysis → embedding
- [x] SearchScreen with semantic/text search toggle and match % scores
- [x] DB version bump to 2 with destructive migration fallback

### Phase 5 — Extras (DONE)
- [x] Manual recording import via system file picker
- [x] ExportHelper — export call summaries as shareable text files
- [x] FileProvider for secure file sharing
- [x] Share/export button on CallDetailScreen
- [x] Processing-complete notification with summary preview
- [ ] On-device STT with whisper.cpp (future enhancement)
- [ ] Dark/light theme toggle (follows system by default)

## Key Decisions
- No call recording — only processes existing recordings from device storage
- User configures recording directory path (default: OnePlus path)
- Cloud STT via Gemini multimodal, cloud LLM via Gemini Flash for analysis
- Semantic search via Gemini text-embedding-004 + local cosine similarity
- Personal/open-source project, not targeting Play Store
