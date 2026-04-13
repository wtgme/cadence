# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This App Does

Cadence is an Android app that reads biometric data (heart rate, GPS speed, steps, sleep, SpO2) and environment context (location, weather, time) to generate personalized instrumental music in real-time via a two-step AI pipeline.

## Build & Test Commands

```bash
adb logcat --pid=$(adb shell pidof -s io.cadence.music.debug) *:V  # Filter logs to app (debug)
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build (minified)
./gradlew test                # Run all unit tests
./gradlew test --tests "io.cadence.music.domain.SceneDetectorTest"  # Single test class
./gradlew lint                # Android Lint
./gradlew clean assembleDebug # Clean build
```

**Setup:** Create `local.properties` in the project root with:
```
songgen.base.url=http://10.0.2.2:8888   # emulator; use http://<phone-ip>:8888 for physical device
openrouter.api.key=<your-key>
```

`songgen.base.url` points at the SongGeneration API (tunnelled to `localhost:8888`). OpenRouter is accessed via `openrouter.api.key`.

## Remote Backend Setup (HPC)

The SongGeneration v2-large inference server runs inside KCL's CREATE HPC cluster. Access requires SSH key auth + MFA via the e-Research Portal.

The **compute node name can change** (currently `erc-hpc-comp232`). Update `REMOTE_HOST` in `scripts/hpc-tunnel.sh` when it does. The working directory `/users/k1810895/data/musicgen` is stable.

### One-time SSH config (already in `~/.ssh/config`)
```
Host CREATE
    Hostname hpc.create.kcl.ac.uk
    User k1810895
    PubkeyAuthentication yes
    IdentityFile ~/.ssh/id_rsa

Host erc-hpc-comp*
    HostName %h
    ProxyJump CREATE
    User k1810895
    PubkeyAuthentication yes
    IdentityFile ~/.ssh/id_rsa
```

### Starting the tunnel (run each session)
```bash
# 1. Authenticate MFA at https://portal.er.kcl.ac.uk/mfa/
# 2. Open the tunnel:
./scripts/hpc-tunnel.sh start

# Check status / connectivity:
./scripts/hpc-tunnel.sh status

# Close when done:
./scripts/hpc-tunnel.sh stop
```

The tunnel forwards:
- `localhost:8888` → `<REMOTE_HOST>:37629` (SongGeneration API)

Claude can run remote commands and read/edit files via the SSH jump host directly:
```bash
ssh -J k1810895@hpc.create.kcl.ac.uk k1810895@erc-hpc-comp232 "<command>"
```

### Remote server details
- **Working dir:** `/users/k1810895/data/musicgen` (stable)
- **Server file:** `songgeneration_server.py`
- **Conda env:** `musicgen` (Python 3.10)
- **Conda path:** `/software/spackages_v0_21_prod/apps/linux-ubuntu22.04-icelake/gcc-13.2.0/anaconda3-2022.10-tjkkt6f5oslpe3qj7vrpvqrm7vru4k6e/bin/conda` (needed for non-interactive SSH; `conda` not in PATH by default)
- **⚠️ Server management:** User starts/stops the Slurm job and API service manually. Claude should ONLY read logs and edit files — never kill processes or use `fuser -k` on the server.
- **Check health:** `curl http://localhost:8888/health`
- **Tail logs:** `ssh -J k1810895@hpc.create.kcl.ac.uk k1810895@erc-hpc-comp232 "tail -f /users/k1810895/data/musicgen/logs/server.log"`

## Architecture

Clean Architecture + MVVM + Hilt DI. Kotlin 2.0.0, compileSdk 36, minSdk 30.

### Two-Step Generation Pipeline

1. **Step 1 (OpenRouter / LLM):** Biometric context → `SongParams` (lyrics, tags, duration, cfg_scale) via `nvidia/nemotron-3-super-120b-a12b:free`. Up to 3 retries with exponential backoff on 429/5xx. Falls back to stub lyrics if unavailable.
2. **Step 2 (SongGeneration v2-large, self-hosted port 8888):** `SongParams` → MP3 file. API: `POST /generate` with `{lyric, descriptions, auto_prompt_audio_type, generate_type}`. `generate_type` is `"bgm"` (instrumental), `"mixed"` (vocals+accompaniment), or `"vocal"`. `auto_prompt_audio_type` can seed a style (Pop, Rock, Electronic, etc.).

### Key Components

| Component | Role |
|-----------|------|
| `MusicOrchestrator` | Singleton; orchestrates scene detection, generation triggering, and playback. Monitors HR drift (±15 bpm) and scene changes to reprime the buffer. |
| `AudioBufferManager` | Maintains a 2-item pre-buffer via an unbounded Channel. Uses epoch-based cache invalidation to discard in-flight generation when context shifts. |
| `SceneDetector` / `SceneStateMachine` | Classifies activity (Running, Walking, Commuting, Stuck in Traffic, Resting) from speed/HR thresholds. State machine adds hysteresis to prevent flicker. |
| `SensorStateCollector` | Aggregates Health Connect data, GPS speed, weather, sleep, and readiness into a single `SensorState`. |
| `MusicPlayerService` | Foreground service; Media3/ExoPlayer playback + notifications. |
| `PromptBuilder` | Formats `SensorState` into an LLM metrics context string. |
| `MusicRepository` | Implements `GenerationRepository`; owns the two-step network calls and JSON parsing. |

### Layers

- **`ui/`** — Compose screens (`PlayerScreen`, `PermissionsScreen`, `DebugScreen`), ViewModels. ViewModels pull state from `MusicOrchestrator` via StateFlow.
- **`audio/`** — `MusicOrchestrator`, `AudioBufferManager`, `MusicPlayerService`, `PlaybackPositionTracker`.
- **`data/api/`** — `GenerationRepository` interface, `MusicRepository` impl, `SongParams` (Moshi).
- **`data/model/`** — `Scene` enum, `SensorState`, `GeneratedSong` (immutable data classes).
- **`data/sensor/`** — All sensor integrations (Health Connect, GPS, weather, sleep).
- **`domain/`** — `SceneDetector`, `SceneStateMachine`, `PromptBuilder`, `ReadinessCalculator`.
- **`di/`** — Hilt modules: `AppModule`, `NetworkModule`, `RepositoryModule`.

### Scene Detection Thresholds

Speed-based: >25 km/h → Commuting/Driving, >8 → Running, >3 → Walking, else Resting. HR override: >135 bpm forces Running regardless of speed.

## Testing

Unit tests cover domain logic and buffer management. Use **Turbine** for StateFlow/Flow assertions.

- `SceneDetectorTest`, `SceneStateMachineTest` — threshold and transition coverage
- `PromptBuilderTest` — formatting assertions
- `AudioBufferManagerTest` — request queueing behavior

## Common Workflows

- **New sensor:** Extend `SensorState` → add retrieval in `SensorStateCollector` → incorporate in `PromptBuilder.buildMetricsContext()`
- **Scene threshold tuning:** Update companion constants in `SceneDetector`
- **Prompt/LLM changes:** Modify `MusicRepository.SYSTEM_INSTRUCTION` and tag vocabularies
- **UI changes:** Compose screens; ViewModels are the bridge to `MusicOrchestrator` state
