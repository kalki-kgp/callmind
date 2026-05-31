#!/usr/bin/env python3
"""Replicates CallMind's GeminiTranscriptionService request to inspect cloud STT output."""
import base64
import json
import os
import sys
import urllib.request
import urllib.error

API_KEY = os.environ["GEMINI_API_KEY"]
AUDIO = sys.argv[1] if len(sys.argv) > 1 else "test/Adam Ahmed-2605292115.mp3"
MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.0-flash")

PROMPT = ("Transcribe this phone call recording accurately. Output ONLY the "
          "transcription text, nothing else. If you can identify different "
          "speakers, prefix their lines with Speaker 1: and Speaker 2: etc. "
          "Transcribe in the language spoken (could be English, Hindi, or mixed).")

ext = AUDIO.rsplit(".", 1)[-1].lower()
mime = {"wav": "audio/wav", "mp3": "audio/mp3", "m4a": "audio/m4a",
        "ogg": "audio/ogg", "amr": "audio/amr", "aac": "audio/aac",
        "flac": "audio/flac"}.get(ext, "audio/wav")

with open(AUDIO, "rb") as f:
    b64 = base64.b64encode(f.read()).decode()

body = json.dumps({
    "contents": [{
        "parts": [
            {"inline_data": {"mime_type": mime, "data": b64}},
            {"text": PROMPT},
        ]
    }]
}).encode()

url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent?key={API_KEY}"
req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})

try:
    with urllib.request.urlopen(req) as resp:
        data = json.load(resp)
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
    sys.exit(1)

text = data["candidates"][0]["content"]["parts"][0]["text"]
usage = data.get("usageMetadata", {})
print("=== MODEL:", MODEL, "===")
print("=== USAGE:", json.dumps(usage), "===")
print("=== TRANSCRIPTION ===")
print(text)
