# Cadence

An Android app that generates music in real time, adapting to your biometrics and surroundings.

## How it works

Cadence reads your heart rate, GPS speed, step count, sleep score, SpO2, and local weather, then runs a two-step AI pipeline to produce audio that matches your physiological state:

1. **Gemma** (self-hosted) — translates live sensor metrics into structured song parameters (genre, BPM, mood, lyric style)
2. **MusicGen** (self-hosted) — generates a 4-minute audio clip from those parameters

Songs are pre-buffered so playback is uninterrupted. When your context shifts — activity level changes, scene changes, or heart rate drifts by ±15 bpm — the buffer is reprimed with fresh music.

## Scenes

Cadence detects your current scene and uses it as additional context for generation:

| Scene | Description |
|---|---|
| Running | High-energy, fast-tempo music matched to cadence |
| Walking | Mid-tempo, steady rhythms |
| Commuting | Chill, alert — lower BPM to avoid overstimulation |
| Stuck in Traffic | Calm or ambient |
| Resting | Slow, restorative, instrumental |

## Self-hosted servers

| Service | Default port | Role |
|---|---|---|
| MusicGen | `8000` | Audio generation |
| Gemma | `8001` | Metrics → song parameters |

Set the server addresses in `local.properties`:

```
SONGGEN_BASE_URL=http://<host>:8000
GEMMA_BASE_URL=http://<host>:8001
```

## Requirements

- Android 10+ (API 29)
- Health Connect for heart rate, sleep, and SpO2
- Location permission for GPS speed and weather

## Build

```bash
./gradlew assembleDebug
```
