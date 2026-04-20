<div align="center">

<img src="screenshots/logo.svg" width="128" alt="Cadence logo"/>

# Cadence

### Biometric-Adaptive Music for Real-Time Mood Regulation

[![Platform](https://img.shields.io/badge/platform-Android%2010%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Google Play](https://img.shields.io/badge/Google%20Play-Under%20Review-414141?logo=googleplay&logoColor=white)](https://play.google.com)

**Cadence** continuously reads your physiological state and generates personalised instrumental music in real time — grounded in the *iso-principle*: matching music to your current state before gradually steering it toward a desired emotional target.

</div>

---

## Screenshots

<table>
  <tr>
    <td align="center"><img src="screenshots/screen_player.png" width="160" height="347"/><br/><sub><b>Player</b><br/>Live scene & heart rate</sub></td>
    <td align="center"><img src="screenshots/screen_reasoning.png" width="160" height="347"/><br/><sub><b>Synthesising</b><br/>Generation in progress</sub></td>
    <td align="center"><img src="screenshots/screen_adjust.png" width="160" height="347"/><br/><sub><b>AI Reasoning Chain</b><br/>Biometric vitals & mental state</sub></td>
    <td align="center"><img src="screenshots/screen_recommendation.png" width="160" height="347"/><br/><sub><b>Taste Profile</b><br/>Generated tracks & preferences</sub></td>
    <td align="center"><img src="screenshots/scene_override_menu.png" width="160" height="347"/><br/><sub><b>Scene Override</b><br/>Manual context control</sub></td>
  </tr>
</table>

---

## Scientific Background

Music is among the most effective strategies for everyday emotion regulation. The **iso-principle** — matching music to a listener's psychophysiological state before shifting it toward a target — has controlled experimental support, producing significantly higher positive affect than passive listening. Neurobiologically, music modulates cortisol, autonomic arousal, and reward circuitry.

These effects depend critically on *fit* between musical properties and real-time listener state. No existing consumer system achieves this automatically. Cadence is a functional prototype addressing this gap.

---

## Two-Step AI Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                      SENSOR LAYER                           │
│  Heart Rate · HRV · SpO2 · Sleep · Steps · GPS · Weather    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               STEP 1 — Context Translation                  │
│  LLM (OpenRouter / Nemotron-3-Super 120B)                   │
│  Biometric context → Mental state estimation                │
│  (arousal · valence · stress · energy · focus)              │
│  → Song parameters (genre tags · BPM · mood · intensity)    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               STEP 2 — Music Generation                     │
│  SongGeneration v2-large (self-hosted)                      │
│  Song parameters → Instrumental MP3                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              PRE-BUFFERED PLAYBACK                          │
│  2-item buffer · seamless transitions                       │
│  Reprimed on scene change or HR drift ±15 bpm               │
└─────────────────────────────────────────────────────────────┘
```

All biometric data is **processed on-device**. Only anonymised contextual summaries are transmitted for music generation.

---

## Scene Detection

Cadence classifies your activity context from sensor fusion and uses it to shape generation:

| Scene | Trigger | Musical intent |
|---|---|---|
| Running | Speed > 8 km/h or HR > 135 bpm | High-energy, tempo-matched |
| Walking | Speed 3–8 km/h | Mid-tempo, steady |
| Commuting | Speed > 25 km/h | Alert, low-distraction |
| Working Out | Manual | Energetic, motivational |
| Focus | Manual | Minimal, concentration-supporting |
| Resting | Low movement / default | Slow, restorative, ambient |
| Party | Manual | Upbeat, social |

---

## Research Transparency

The app exposes its full reasoning chain in real time so users understand *why* a piece of music was generated:

- **Biometric input** — raw sensor readings with weather and location context
- **Mental state estimation** — scored dimensions: arousal, valence, stress, energy, focus, mood
- **Music recommendation** — selected style, genre tags, and lyric scaffold
- **Override & feedback** — users may adjust any parameter or rate tracks; a taste profile adapts over time

This design supports informed consent and user agency — principles central to responsible AI deployment in health contexts.

---

## Requirements

- Android 10+ (API 30)
- [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) — heart rate, HRV, sleep, SpO2
- Location permission — GPS speed and weather
- A compatible wearable: Fitbit, Samsung Galaxy Watch, Pixel Watch, or any Health Connect–compatible device

---

## Setup

Create `local.properties` in the project root:

```
songgen.base.url=http://10.0.2.2:8888   # emulator
# songgen.base.url=http://<device-ip>:8888  # physical device
openrouter.api.key=<your-key>
```

### Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (minified)
./gradlew test              # unit tests
```

---

## Architecture

Clean Architecture · MVVM · Hilt DI · Kotlin 2.0 · Jetpack Compose · Media3/ExoPlayer

```
app/
├── ui/           Compose screens (Player, Debug, Permissions) + ViewModels
├── audio/        MusicOrchestrator · AudioBufferManager · MusicPlayerService
├── data/
│   ├── api/      GenerationRepository · MusicRepository · SongParams (Moshi)
│   ├── model/    Scene · SensorState · GeneratedSong
│   └── sensor/   Health Connect · GPS · Weather · Sleep integrations
├── domain/       SceneDetector · SceneStateMachine · PromptBuilder · ReadinessCalculator
└── di/           Hilt modules
```

---

<div align="center">

*Cadence is under review for release on the Google Play Store.*

</div>
