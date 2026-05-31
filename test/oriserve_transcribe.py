#!/usr/bin/env python3
"""Runs Oriserve Whisper-Hindi2Hinglish locally to inspect on-device STT output type."""
import sys
import time
import torch
from transformers import pipeline

MODEL = sys.argv[2] if len(sys.argv) > 2 else "Oriserve/Whisper-Hindi2Hinglish-Swift"
AUDIO = sys.argv[1] if len(sys.argv) > 1 else "test/Adam Ahmed-2605292115.mp3"

device = "mps" if torch.backends.mps.is_available() else "cpu"
print(f"=== MODEL: {MODEL} | device: {device} ===", flush=True)

t0 = time.time()
asr = pipeline(
    "automatic-speech-recognition",
    model=MODEL,
    device=device,
    torch_dtype=torch.float32,
)
load_s = time.time() - t0

t1 = time.time()
result = asr(
    AUDIO,
    chunk_length_s=30,
    return_timestamps=True,
    generate_kwargs={"task": "transcribe"},
)
infer_s = time.time() - t1

print(f"=== load: {load_s:.1f}s | inference: {infer_s:.1f}s ===")
print("=== TRANSCRIPTION ===")
print(result["text"].strip())
if "chunks" in result:
    print("=== SEGMENTS ===")
    for c in result["chunks"]:
        print(c.get("timestamp"), c.get("text"))
