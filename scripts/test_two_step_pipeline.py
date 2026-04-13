#!/usr/bin/env python3
"""
A/B test: single-query vs two-query LLM pipeline for SongParams generation.

Single-query:  sensor metrics → [music producer LLM] → SongParams
Two-query:     sensor metrics → [psychophysiologist LLM] → MentalState
               MentalState   → [music producer LLM]     → SongParams

Usage:
    python3 scripts/test_two_step_pipeline.py

Reads OPENROUTER_API_KEY from local.properties automatically.
"""

import json
import os
import re
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path

# ── Config ────────────────────────────────────────────────────────────────────

OPENROUTER_BASE = "https://openrouter.ai/api/v1/chat/completions"
MODEL           = "nvidia/nemotron-3-super-120b-a12b:free"

AUTO_PROMPT_TYPES = {
    "Pop", "Latin", "Rock", "Electronic", "Metal", "Country",
    "R&B/Soul", "Ballad", "Jazz", "World", "Hip-Hop", "Funk", "Soundtrack", "Auto",
}

# ── Load API key from local.properties ───────────────────────────────────────

def load_api_key() -> str:
    props_path = Path(__file__).parent.parent / "local.properties"
    if props_path.exists():
        text = props_path.read_text()
        m = re.search(r"openrouter\.api\.key\s*=\s*(.+)", text)
        if m:
            return m.group(1).strip()
    key = os.environ.get("OPENROUTER_API_KEY", "")
    if not key:
        sys.exit("ERROR: openrouter.api.key not found in local.properties and OPENROUTER_API_KEY env not set")
    return key

# ── Representative sensor snapshots ──────────────────────────────────────────

SNAPSHOTS = [
    {
        "label": "Running / high HR / morning sunny",
        "metrics": (
            "Activity: Running, GPS Speed: 10.2 km/h, Weather: sunny — favour major key, bright valence, higher energy, "
            "HR: 162 bpm, SpO2: 98%, Location: 51.5074, -0.1278, "
            "Today: 4200 steps, 32 mins, 310 kcal, Readiness: 82/100 (sleep:28,hrv:27,rhr:27), "
            "Sleep: Well-rested (84/100), deep 18%, REM 22%, "
            "Time: Early morning (7:30am), Day: Wednesday (weekday)\n"
            "Music guidance: Energy tier: Very High — target 145+ BPM (sympathetic drive)."
        ),
    },
    {
        "label": "Commuting / moderate HR / overcast morning",
        "metrics": (
            "Activity: Driving/Commuting, GPS Speed: 42.5 km/h, Weather: overcast — soothing, acoustic, neutral valence, "
            "HR: 78 bpm, SpO2: 97%, Location: 51.5074, -0.1278, "
            "Today: 800 steps, 5 mins, 45 kcal, Readiness: 61/100 (sleep:20,hrv:21,rhr:20), "
            "Sleep: Average sleep (58/100), deep 10%, REM 17%, "
            "Time: Morning (8:45am), Day: Monday (weekday)\n"
            "Music guidance: Energy tier: High — target 110–130 BPM (flow state)."
        ),
    },
    {
        "label": "Resting / low HR / late night",
        "metrics": (
            "Activity: Resting, GPS Speed: 0.0 km/h, Weather: clear — favour major key, bright valence, higher energy, "
            "HR: 54 bpm, SpO2: 96%, Location: 51.5074, -0.1278, "
            "Today: 9800 steps, 65 mins, 620 kcal, Readiness: 72/100 (sleep:24,hrv:24,rhr:24), "
            "Sleep: Well-rested (76/100), deep 20%, REM 23%, "
            "Time: Night (11:15pm), Day: Friday (weekday)\n"
            "Music guidance: Energy tier: Low — target <60 BPM (parasympathetic rebound)."
        ),
    },
    {
        "label": "Walking / medium HR / rainy evening",
        "metrics": (
            "Activity: Walking, GPS Speed: 5.1 km/h, Weather: rainy — favour minor key, introspective, lower energy, "
            "HR: 95 bpm, SpO2: 97%, Location: 51.5074, -0.1278, "
            "Today: 7500 steps, 55 mins, 480 kcal, Readiness: 44/100 (sleep:14,hrv:15,rhr:15), "
            "Sleep: Poorly rested (38/100), deep 7%, REM 12%, "
            "Time: Evening (6:30pm), Day: Thursday (weekday)\n"
            "Music guidance: Readiness capacity: Low — capped to Medium by current context (target 90–110 BPM (active recovery)). "
            "Low REM sleep (12%) — use simple melodies, high melodic clarity. "
            "Low deep sleep (7%) — reduce percussive density, avoid heavy drums."
        ),
    },
    {
        "label": "Post-workout / elevated HR / sunny afternoon",
        "metrics": (
            "Activity: Resting, GPS Speed: 0.3 km/h, Weather: sunny — favour major key, bright valence, higher energy, "
            "HR: 110 bpm, SpO2: 98%, Location: 51.5074, -0.1278, "
            "Today: 12400 steps, 90 mins, 950 kcal, Readiness: 55/100 (sleep:18,hrv:19,rhr:18), "
            "Sleep: Average sleep (62/100), deep 14%, REM 20%, "
            "Time: Afternoon (3:45pm), Day: Saturday (weekend)\n"
            "Music guidance: Energy tier: Medium — target 90–110 BPM (active recovery)."
        ),
    },
    {
        "label": "Focused desk work / low-normal HR / afternoon cloudy",
        "metrics": (
            "Activity: Stationary, GPS Speed: 0.0 km/h, Weather: overcast — soothing, acoustic, neutral valence, "
            "HR: 68 bpm, SpO2: 97%, Location: 51.5074, -0.1278, "
            "Today: 2100 steps, 10 mins, 120 kcal, Readiness: 78/100 (sleep:26,hrv:26,rhr:26), "
            "Sleep: Well-rested (80/100), deep 19%, REM 24%, "
            "Time: Afternoon (2:15pm), Day: Tuesday (weekday)\n"
            "Music guidance: Energy tier: High — target 110–130 BPM (flow state)."
        ),
    },
]

# ── Prompts ───────────────────────────────────────────────────────────────────

SINGLE_QUERY_SYSTEM = """
You are a biometric-aware music producer. Translate a real-time sensor snapshot into
music style parameters for an AI music generation model. Output ONLY a valid JSON
object — no explanation, no markdown fences.

JSON fields:
  "descriptions": 3–6 comma-separated lowercase tags from these dimensions:
      Genre    : pop, jazz, rock, electronic, ambient, classical, funk, r&b, hip-hop, folk, new-age, blues
      Emotion  : energetic, calm, peaceful, uplifting, melancholic, introspective, focused,
                 euphoric, powerful, dreamy, relaxing, sad, cheerful, romantic
      Instrument: piano, synthesizer, electric guitar, acoustic guitar, drums, drum machine,
                  bass guitar, strings, violin, saxophone, trumpet, flute
  "auto_prompt_audio_type": MUST be exactly one of:
      Pop, Latin, Rock, Electronic, Metal, Country, R&B/Soul, Ballad, Jazz, World, Hip-Hop, Funk, Soundtrack, Auto
      NOTE: "Ambient" is NOT valid — use Soundtrack instead.

Encode tempo through genre — do NOT use words like fast/slow/mid-tempo/driving/upbeat:
  145+ BPM → electronic or rock + energetic/powerful + drums
  110–130 BPM → pop or funk + energetic/uplifting + bass guitar
  90–110 BPM  → jazz or folk + focused/cheerful + piano or saxophone
  <60 BPM     → ambient or classical or new-age + calm/peaceful/dreamy + piano or strings

Rules:
  - Follow the Energy Tier in Music guidance exactly
  - Sunny weather → add uplifting or euphoric; rainy → add melancholic or introspective (not both)
  - Low REM / low deep sleep flags → no drums or drum machine; use piano, strings, or acoustic guitar
  - Night/evening → add dreamy or introspective; no drum machine
  - Never mix contradictory emotions (e.g. calm + energetic)
""".strip()

MENTAL_STATE_SYSTEM = """
You are a psychophysiologist. Given real-time biometric sensor data, estimate the user's
current mental and physiological state. Output ONLY a valid JSON object — no explanation,
no markdown fences.

JSON fields:
  "arousal":  integer 0–10  (0 = completely calm/sedated, 10 = maximally activated/agitated)
  "valence":  integer -5 to +5  (-5 = very negative/distressed, 0 = neutral, +5 = very positive/elated)
  "stress":   integer 0–10  (0 = no stress, 10 = extreme stress)
  "energy":   integer 0–10  (0 = exhausted, 10 = fully energised)
  "focus":    integer 0–10  (0 = scattered/drowsy, 10 = deeply focused)
  "mood":     string — one short phrase (e.g. "alert and motivated", "calm and content", "tired but winding down")

Use the full range of values. Be precise — a well-rested morning run should differ
clearly from a stressed commute or a late-night rest.
""".strip()

SONG_PARAMS_FROM_MENTAL_STATE_SYSTEM = """
You are a music producer. Given a user's current mental and physiological state,
recommend instrumental music parameters for an AI music generation model.
Output ONLY a valid JSON object — no explanation, no markdown fences.

JSON fields:
  "descriptions": 3–6 comma-separated lowercase tags from these dimensions:
      Genre    : pop, jazz, rock, electronic, ambient, classical, funk, r&b, hip-hop, folk, new-age, blues
      Emotion  : energetic, calm, peaceful, uplifting, melancholic, introspective, focused,
                 euphoric, powerful, dreamy, relaxing, sad, cheerful, romantic
      Instrument: piano, synthesizer, electric guitar, acoustic guitar, drums, drum machine,
                  bass guitar, strings, violin, saxophone, trumpet, flute
  "auto_prompt_audio_type": MUST be exactly one of:
      Pop, Latin, Rock, Electronic, Metal, Country, R&B/Soul, Ballad, Jazz, World, Hip-Hop, Funk, Soundtrack, Auto
      NOTE: "Ambient" is NOT valid — use Soundtrack instead.

Use iso-principle: match the music energy to the user's current arousal first, then
gently guide toward a positive valence. High stress → prefer calming genres (jazz, ambient,
new-age) unless arousal is very high. High energy + positive valence → electronic/pop.
Low energy + low valence → ambient/classical with peaceful tags.

Encode tempo through genre — do NOT use words like fast/slow/mid-tempo/driving/upbeat:
  arousal 8–10 → electronic or rock + energetic/powerful + drums (145+ BPM equivalent)
  arousal 5–7  → pop or funk + energetic/uplifting + bass guitar (110–130 BPM equivalent)
  arousal 3–4  → jazz or folk + focused/cheerful + piano (90–110 BPM equivalent)
  arousal 0–2  → ambient or classical + calm/peaceful/dreamy + piano or strings (<60 BPM equivalent)

Rules:
  - If stress >= 7: prefer calming genres (jazz, ambient, new-age) regardless of arousal
  - If focus >= 7: add "focused" tag; prefer piano, strings, or acoustic guitar
  - If valence <= -2: add melancholic or introspective (not both)
  - If valence >= 3: add uplifting or euphoric (not both)
  - Never mix contradictory emotions (e.g. calm + energetic)
""".strip()

# ── OpenRouter API call ───────────────────────────────────────────────────────

def call_openrouter(api_key: str, system: str, user_msg: str, label: str) -> tuple[str | None, float]:
    payload = json.dumps({
        "model": MODEL,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user",   "content": user_msg},
        ],
        "temperature": 0.7,
    }).encode()

    req = urllib.request.Request(
        OPENROUTER_BASE,
        data=payload,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://cadence.music",
            "X-Title": "Cadence",
        },
        method="POST",
    )

    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            elapsed = time.time() - t0
            raw = resp.read().decode()
        data = json.loads(raw)
        content = data["choices"][0]["message"]["content"].strip()
        return content, elapsed
    except urllib.error.HTTPError as e:
        elapsed = time.time() - t0
        body = e.read().decode()[:300]
        print(f"  !! {label}: HTTP {e.code} in {elapsed:.1f}s — {body}")
        return None, elapsed
    except Exception as e:
        elapsed = time.time() - t0
        print(f"  !! {label}: {e} in {elapsed:.1f}s")
        return None, elapsed

# ── JSON extraction ───────────────────────────────────────────────────────────

def extract_json(text: str) -> dict | None:
    # Strip markdown fences
    fence_open = text.find("```")
    fence_close = text.rfind("```")
    if fence_open >= 0 and fence_close > fence_open:
        start = text.find("\n", fence_open)
        start = (start + 1) if start >= 0 else fence_open + 3
        text = text[start:fence_close].strip()

    brace_start = text.find("{")
    brace_end   = text.rfind("}")
    if brace_start >= 0 and brace_end > brace_start:
        text = text[brace_start:brace_end + 1]

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return None

def validate_song_params(d: dict | None) -> dict | None:
    if not d:
        return None
    descriptions = d.get("descriptions") or d.get("tags")
    if not descriptions:
        return None
    apt = d.get("auto_prompt_audio_type", "")
    if apt not in AUTO_PROMPT_TYPES:
        d["auto_prompt_audio_type"] = f"{apt} ⚠ INVALID"
    return d

# ── Run one snapshot ──────────────────────────────────────────────────────────

def run_snapshot(api_key: str, snap: dict) -> None:
    label   = snap["label"]
    metrics = snap["metrics"]

    print(f"\n{'═' * 70}")
    print(f"  {label}")
    print(f"{'═' * 70}")

    # ── Single-query ──────────────────────────────────────────────────────
    print("\n[SINGLE-QUERY]")
    sq_raw, sq_time = call_openrouter(
        api_key,
        SINGLE_QUERY_SYSTEM,
        f"Biometric & environmental snapshot:\n{metrics}",
        "single-query",
    )
    sq_params = validate_song_params(extract_json(sq_raw)) if sq_raw else None
    if sq_params:
        print(f"  descriptions         : {sq_params.get('descriptions')}")
        print(f"  auto_prompt_audio_type: {sq_params.get('auto_prompt_audio_type')}")
    else:
        print(f"  [FAILED to parse] raw={str(sq_raw)[:200]}")
    print(f"  latency: {sq_time:.1f}s")

    # ── Two-query ─────────────────────────────────────────────────────────
    print("\n[TWO-QUERY — step 1: mental state]")
    ms_raw, ms_time = call_openrouter(
        api_key,
        MENTAL_STATE_SYSTEM,
        f"Biometric sensor snapshot:\n{metrics}",
        "two-query/step1",
    )
    mental_state = extract_json(ms_raw) if ms_raw else None
    if mental_state:
        print(f"  arousal={mental_state.get('arousal')}  valence={mental_state.get('valence')}  "
              f"stress={mental_state.get('stress')}  energy={mental_state.get('energy')}  "
              f"focus={mental_state.get('focus')}")
        print(f"  mood: {mental_state.get('mood')}")
    else:
        print(f"  [FAILED to parse mental state] raw={str(ms_raw)[:200]}")
    print(f"  latency: {ms_time:.1f}s")

    tq_params = None
    tq_time   = 0.0
    if mental_state:
        print("\n[TWO-QUERY — step 2: song params from mental state]")
        tq_raw, tq_time = call_openrouter(
            api_key,
            SONG_PARAMS_FROM_MENTAL_STATE_SYSTEM,
            f"User's current mental state:\n{json.dumps(mental_state, indent=2)}",
            "two-query/step2",
        )
        tq_params = validate_song_params(extract_json(tq_raw)) if tq_raw else None
        if tq_params:
            print(f"  descriptions         : {tq_params.get('descriptions')}")
            print(f"  auto_prompt_audio_type: {tq_params.get('auto_prompt_audio_type')}")
        else:
            print(f"  [FAILED to parse] raw={str(tq_raw)[:200]}")
        print(f"  latency: {tq_time:.1f}s  (total two-query: {ms_time + tq_time:.1f}s)")

    # ── Side-by-side ──────────────────────────────────────────────────────
    print("\n[COMPARISON]")
    sq_desc = sq_params.get("descriptions", "—") if sq_params else "FAILED"
    tq_desc = tq_params.get("descriptions", "—") if tq_params else "FAILED"
    sq_apt  = sq_params.get("auto_prompt_audio_type", "—") if sq_params else "FAILED"
    tq_apt  = tq_params.get("auto_prompt_audio_type", "—") if tq_params else "FAILED"

    print(f"  {'':28} {'SINGLE':>30}  {'TWO-QUERY':>30}")
    print(f"  {'descriptions':<28} {str(sq_desc):>30}  {str(tq_desc):>30}")
    print(f"  {'auto_prompt_audio_type':<28} {str(sq_apt):>30}  {str(tq_apt):>30}")
    print(f"  {'total latency':<28} {sq_time:>29.1f}s  {ms_time + tq_time:>29.1f}s")

# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    api_key = load_api_key()
    print(f"Model: {MODEL}")
    print(f"Snapshots: {len(SNAPSHOTS)}")
    print(f"API key: {api_key[:12]}…")

    for snap in SNAPSHOTS:
        run_snapshot(api_key, snap)

    print(f"\n{'═' * 70}")
    print("Done.")

if __name__ == "__main__":
    main()
