# To Be Implemented

Native, on-device work that is designed for but not yet wired into the shipping
app. Both items follow the same runtime-download pattern as `VoskModelManager`
(download to `filesDir`, never bundle the model in the APK).

---

## 1. Local STT — Oriserve Whisper-Hindi2Hinglish "Apex" via whisper.cpp

**Status:** deferred. Cloud STT (`GeminiTranscriptionService`,
`gemini-flash-lite-latest`) is the only STT path shipping today. The Vosk path
still exists but its Hinglish quality is unusable and it should be removed once
Apex lands.

### Why Apex (not Swift / Prime)
- On a real 20s Hinglish call clip, **Apex** matched Gemini on every critical
  fact and fixed all meaning-inversions that **Swift** introduced.
- On conversational-speech WER (IndicVoices): Apex **47.6** beats Swift 65.2 and
  Prime 60.8.
- **Prime** (3.7 GB) is un-shippable on a phone. **Apex** (~0.8B, large-v3-turbo
  class) quantizes to GGML `q8_0` ≈ 870 MB, ~1–1.3 GB RAM, ~0.5–1.5× realtime on
  the OnePlus Nord 5 (SD 7+ Gen 3). Fine for the batch WorkManager pipeline; not
  for live transcription.

### Work required
1. **Convert the fine-tune to GGML.** Take `Oriserve/Whisper-Hindi2Hinglish-Apex`,
   convert HF → GGML, quantize to `q8_0`. Validate on a real clip first —
   fine-tune conversions sometimes need a tokenizer fixup before they decode
   correctly. Host the resulting `.bin` for runtime download.
2. **whisper.cpp via NDK/JNI.** Add whisper.cpp as a native module (CMake +
   `externalNativeBuild` in `app/build.gradle.kts`), with a JNI bridge exposing
   `transcribe(pcmFloatArray, lang) -> String`. Reuse `AudioConverter` to feed
   16 kHz mono PCM (same preprocessing Vosk needs today).
3. **`ApexModelManager`** mirroring `VoskModelManager`: runtime download of the
   GGML `.bin`, `isModelDownloaded`, cached handle, progress callback.
4. **`ApexTranscriptionService`** implementing the same surface as
   `VoskTranscriptionService` / `GeminiTranscriptionService`.
5. **Make STT provider truly selectable.** Today `UserPreferences.useLocalStt` is
   a boolean (Gemini vs Vosk). Generalize to an `stt_provider` string
   (`"gemini"` / `"apex"`), mirror the embedding-provider refactor in this repo
   (see `EmbeddingProvider` + `EmbeddingProviderRegistry`), and update
   `TranscriptionWorker` + the Settings UI download/gating to match.

### Pattern to copy
The embedding work already in this repo is the template:
- `EmbeddingProvider` (interface) + `EmbeddingProviderRegistry` (pref-driven pick)
- `EmbeddingModelManager` (runtime download)
- Settings card with provider chips + gated download button.

---

## 2. Upgrade local embeddings to EmbeddingGemma (LiteRT)

**Status:** the selectable embedding architecture is **shipped** (cloud
`gemini-embedding-001` + on-device via MediaPipe `TextEmbedder`). The on-device
model currently downloaded is the **Universal Sentence Encoder** — reliably
shippable but English-leaning, so Hinglish/Hindi search quality on-device is
weaker than the cloud path.

### Goal
Replace the on-device model with **`google/embeddinggemma-300m`** (308M params,
768-dim Matryoshka, <200 MB RAM quantized) — best Hindi/Hinglish quality under
500M with an official Android runtime.

### Why it's not done yet
EmbeddingGemma ships as a **LiteRT / Google AI Edge** artifact, not a MediaPipe
`TextEmbedder` `.tflite` bundle with the metadata that runtime expects. It needs
the **LiteRT runtime** (or the Google AI Edge RAG library,
`litert-community/embeddinggemma-300m`), which is a different integration and
needs on-device validation that can't be done without the target hardware.

### Work required
1. Add the LiteRT / Google AI Edge RAG dependency and confirm the EmbeddingGemma
   `.litertlm` (or equivalent) asset + tokenizer load on the Nord 5.
2. Implement a second `EmbeddingProvider` backed by LiteRT (drop-in alongside the
   existing `LocalEmbeddingService`); point `EmbeddingModelManager` at the
   EmbeddingGemma asset, or add a parallel manager.
3. **Calibrate `searchThreshold`** for EmbeddingGemma — it will differ from both
   the cloud floor (0.5) and the current USE placeholder (0.6).
4. **Re-embed.** Vectors from a different model aren't comparable even at the same
   dimension; `modelUsed` stamping + the same-model filter in
   `SemanticSearchEngine` already enforce this, so existing rows simply won't
   match until re-processed. Surface a "re-index" action, or re-process on model
   change.
- Fallback if EmbeddingGemma proves impractical on-device: `multilingual-e5-small`
  (ONNX) via onnxruntime-android.
