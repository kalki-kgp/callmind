#!/usr/bin/env python3
"""Replicates EmbeddingWorker.chunkText + GeminiEmbeddingService + SemanticSearchEngine end-to-end."""
import json
import math
import os
import re
import sys
import urllib.request
import urllib.error

API_KEY = os.environ["GEMINI_API_KEY"]
EMBED_MODEL = "models/gemini-embedding-001"  # text-embedding-004 retired
EMBED_DIM = 768  # match app's original 768-dim vectors via outputDimensionality
THRESHOLD = 0.25  # SemanticSearchEngine default used by SearchViewModel

TRANSCRIPT_FILE = sys.argv[1] if len(sys.argv) > 1 else "out_gemini_transcript.txt"

QUERIES = [
    "where are they meeting?",          # relevant -> garden / gate
    "smoking break",                    # relevant-ish -> sutta
    "what is the address",              # relevant -> gate in front of house
    "pizza delivery order",             # irrelevant -> should be below threshold
    "salary negotiation",               # irrelevant -> should be below threshold
]


def chunk_text(text, max_chunk=500):
    """Mirror of EmbeddingWorker.chunkText (sentence split incl. Hindi danda)."""
    if len(text) <= max_chunk:
        return [text]
    sentences = re.split(r"(?<=[.!?।])\s+", text)
    chunks, cur = [], ""
    for s in sentences:
        if len(cur) + len(s) > max_chunk and cur:
            chunks.append(cur.strip())
            cur = ""
        cur += s + " "
    if cur.strip():
        chunks.append(cur.strip())
    return [c for c in chunks if c.strip()]


def embed_one(text):
    body = json.dumps({
        "model": EMBED_MODEL,
        "content": {"parts": [{"text": text}]},
        "outputDimensionality": EMBED_DIM,
    }).encode()
    url = f"https://generativelanguage.googleapis.com/v1beta/{EMBED_MODEL}:embedContent?key={API_KEY}"
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.load(resp)
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)
    return data["embedding"]["values"]


def embed_batch(texts):
    return [embed_one(t) for t in texts]


def cosine(a, b):
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(y * y for y in b))
    return 0.0 if na == 0 or nb == 0 else dot / (na * nb)


with open(TRANSCRIPT_FILE) as f:
    transcript = f.read().strip()

chunks = chunk_text(transcript)
print(f"=== {len(chunks)} chunk(s) from transcript ===")
for i, c in enumerate(chunks):
    print(f"  [{i}] ({len(c)} chars) {c[:80]}...")

chunk_vecs = embed_batch(chunks)
print(f"=== embedded: {len(chunk_vecs)} vectors, dim={len(chunk_vecs[0])} ===\n")

query_vecs = embed_batch(QUERIES)
for q, qv in zip(QUERIES, query_vecs):
    scored = sorted(
        ((cosine(qv, cv), i, chunks[i]) for i, cv in enumerate(chunk_vecs)),
        reverse=True,
    )
    top_score = scored[0][0]
    hit = "MATCH " if top_score >= THRESHOLD else "below "
    print(f"[{hit}{top_score:.3f}] q={q!r}")
    for score, i, text in scored[:2]:
        mark = "+" if score >= THRESHOLD else "-"
        print(f"      {mark} {score:.3f}  chunk[{i}]: {text[:70]}")
    print()
