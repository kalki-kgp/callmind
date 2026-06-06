package com.callmind.app.data.local.db.entity

/**
 * The active stage of the processing pipeline for a single call.
 *
 * Persisted on [CallEntity.processingStage] as the enum name so the live stage
 * survives process death and drives every surface (list, detail, notification)
 * from one source of truth.
 *
 * [progress] is an honest, stage-mapped fraction in 0..1 — we can't know the
 * sub-progress of a remote STT/LLM call, so each stage advances the bar to a
 * representative point and the active stage animates in place.
 *
 * [step] maps onto the three user-visible pipeline steps (1 Transcribe,
 * 2 Analyze, 3 Index); 0 = queued, 4 = finished, -1 = failed.
 */
enum class ProcessingStage(
    val progress: Float,
    val label: String,
    val activeLabel: String,
    val step: Int
) {
    QUEUED(0.04f, "Queued", "Queued", 0),
    TRANSCRIBING(0.16f, "Transcribe", "Transcribing", 1),
    ANALYZING(0.52f, "Analyze", "Analyzing", 2),
    EMBEDDING(0.84f, "Index", "Indexing", 3),
    COMPLETED(1f, "Done", "Done", 4),
    FAILED(0f, "Failed", "Failed", -1);

    val isActive: Boolean get() = this != COMPLETED && this != FAILED

    companion object {
        /** Number of user-visible pipeline steps shown in the stepped indicator. */
        const val STEP_COUNT = 3

        fun fromName(name: String?): ProcessingStage? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
