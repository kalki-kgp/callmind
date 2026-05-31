#!/usr/bin/env python3
"""Replicates CallMind's AnalysisWorker prompt + OpenAiCompatibleService request (Nebius)."""
import json
import os
import sys
import urllib.request
import urllib.error

API_KEY = os.environ["NEBIUS_API_KEY"]
MODEL = os.environ.get("NEBIUS_MODEL", "deepseek-ai/DeepSeek-V3-0324-fast")
BASE_URL = os.environ.get("NEBIUS_BASE_URL", "https://api.tokenfactory.us-central1.nebius.com/v1/")
CONTACT = os.environ.get("CONTACT", "Adam Ahmed")

# Transcript: from file arg, else stdin
if len(sys.argv) > 1:
    with open(sys.argv[1]) as f:
        transcript = f.read().strip()
else:
    transcript = sys.stdin.read().strip()

PROMPT = f"""You are analyzing a phone call transcript. Return ONLY valid JSON (no markdown, no explanation) with this exact structure:

{{
  "summary": "2-3 sentence summary of the call",
  "sentiment": "POSITIVE or NEGATIVE or NEUTRAL or MIXED",
  "topics": ["topic1", "topic2"],
  "action_items": ["action item 1", "action item 2"],
  "key_points": ["key point 1", "key point 2"]
}}

Rules:
- summary: concise, captures the main purpose and outcome of the call
- sentiment: overall emotional tone of the conversation
- topics: 2-5 short topic labels (e.g. "salary negotiation", "project deadline")
- action_items: specific commitments or tasks mentioned (who needs to do what)
- key_points: important facts or decisions from the conversation

Contact name: {CONTACT}

Transcript:
{transcript}"""

body = json.dumps({
    "model": MODEL,
    "messages": [{"role": "user", "content": PROMPT}],
}).encode()

url = BASE_URL.rstrip("/") + "/chat/completions"
req = urllib.request.Request(
    url, data=body,
    headers={"Authorization": f"Bearer {API_KEY}", "Content-Type": "application/json"},
)

try:
    with urllib.request.urlopen(req) as resp:
        data = json.load(resp)
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
    sys.exit(1)

content = data["choices"][0]["message"]["content"]
print("=== MODEL:", MODEL, "===")
print("=== RAW LLM OUTPUT ===")
print(content)
print("=== PARSED (after stripping markdown fences) ===")
cleaned = content.replace("```json", "").replace("```", "").strip()
try:
    print(json.dumps(json.loads(cleaned), indent=2, ensure_ascii=False))
except Exception as e:
    print(f"[JSON parse failed: {e}] -> app would fall back to summary=raw text")
