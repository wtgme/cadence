# Health Connect Declaration — Cadence AI Music

Submit at: https://support.google.com/googleplay/android-developer/contact/hcrequest

Google reviews this separately from the main app submission. Typical turnaround: 2–4 weeks. **Start this as early as possible.**

---

## App basics

| Field | Value |
|---|---|
| App name | Cadence AI Music |
| Package name | `io.cadence.music` |
| Play Store listing URL | (paste once you have an internal-test listing) |
| Developer name | [YOUR FULL LEGAL NAME] |
| Contact email | [YOUR CONTACT EMAIL] |

---

## Main declaration

### What is the primary purpose of your app's use of Health Connect?

> Cadence AI Music generates instrumental background music that adapts to the user's current physiological state in real time. The app reads biometric data (heart rate, sleep quality, activity level, SpO₂) from Health Connect and combines it with environmental context (GPS-derived activity scene, local weather, time of day) to estimate the user's current state — arousal, valence, stress, energy, focus — and generate a matching music track. The music is intended as a wellbeing aid in the tradition of the iso-principle used in music therapy: match the listener's current state, then gently shift it toward a more regulated target.
>
> Health Connect is the sole source of biometric data; the app does not implement its own sensor integrations. All raw readings are processed on-device. Only an anonymised, aggregated snapshot (e.g. "heart rate 92 bpm, walking, 18:30, overcast") is transmitted over HTTPS to the music-generation backend, where it is processed in memory to produce a musical style and then discarded. No biometric values are stored on any server.

---

## Per-data-type justifications

For each Health Connect data type the app requests, Google will ask:
1. Why the app needs it
2. What user-facing feature it powers
3. Whether it is read or write
4. Whether it is transmitted off-device

All of the below are **read-only** (the app never writes to Health Connect) and only a **summary** (never the raw history) is transmitted off-device.

### Heart rate
> **Feature:** Real-time music generation and automatic scene detection. A heart rate above 135 bpm overrides other signals to classify the activity as "running". Heart-rate drift of ≥15 bpm triggers a music re-generation matched to the new intensity.
> **Justification for transmission:** The current heart rate is included in the anonymised summary sent to the generation backend so the produced music matches the user's current arousal level. No history or timestamp series is transmitted — only the current value.

### Heart-rate variability (HRV)
> **Feature:** Daily readiness score computation. HRV trend compared against a personal baseline is one input to a readiness score that influences whether the app selects energising or restorative music.
> **Justification for transmission:** Same as heart rate — the current snapshot (current HRV and a rolling baseline) is part of the anonymised summary.

### Sleep
> **Feature:** Daily readiness score and music-style selection. Total sleep duration, deep-sleep percentage, and REM percentage feed into the readiness score and influence music choice (e.g. low deep-sleep percentage → avoid percussive genres; prefer piano, strings).
> **Justification for transmission:** A summary of the most recent sleep session (durations and percentages) is part of the anonymised summary. Individual sleep-stage timings are never transmitted.

### Blood oxygen (SpO₂)
> **Feature:** Physiological distress detection. SpO₂ below 94% indicates possible physiological stress and causes the app to prioritise calming, low-intensity musical styles.
> **Justification for transmission:** The current SpO₂ value is part of the anonymised summary.

### Steps / Distance / Active calories
> **Feature:** Activity scene detection and daily readiness score.
> **Justification for transmission:** The current day's aggregated activity totals are part of the anonymised summary.

### Body temperature (optional)
> **Feature:** Used only if available from the user's wearable; contributes to the readiness score.

### Blood pressure (optional)
> **Feature:** Used only if available; displayed in the app's "Reasoning" screen for transparency to the user.

### Resting heart rate
> **Feature:** Daily readiness score baseline.

---

## Data handling

### How is data stored?
> Biometric data read from Health Connect is held in memory for the duration of a single music-generation request (typically under 60 seconds) and then released. The app does not persist any Health Connect data to disk, local database, cloud storage, or shared preferences.

### How is data transmitted?
> An anonymised summary — containing only current sensor values, with no identifiers, no user ID, no device ID, and no timestamp history — is sent over HTTPS (TLS 1.2+) to the music-generation backend (`api.cadencemusics.uk`). The backend is operated by the app developer.

### Who can access the data?
> Only the music-generation backend, which processes the request in memory and returns a music track. No analytics, advertising, or third-party SDKs receive Health Connect data. Service logs on the backend contain a timestamp, a request ID, and the model version — no biometric values.

### Retention?
> Backend: not retained beyond the request. On-device: not retained (read-through only; Health Connect is the system of record).

### Deletion?
> Users delete all app-related data by uninstalling the app or clearing app data in Android Settings. Health Connect permissions can be revoked at any time in Android Settings → Apps → Health Connect → App permissions.

---

## Checklist — what to attach to the Health Connect form

- [ ] Screen recording showing the user permission prompts (one per data type)
- [ ] Screen recording showing the data being used (e.g., the Reasoning screen displaying current HR, sleep, etc.)
- [ ] Screenshot of the app's privacy policy page highlighting the Health Connect section
- [ ] Link to the Play Store internal-test listing (needs to be live first)

Google asks you to show that each data type is actually used in a user-visible feature. The Reasoning screen in Cadence is a perfect asset for this.

---

## Per-permission description strings (copy-paste into Play Console)

Google Play's Health Connect declaration form asks **"Describe your app's use of [permission]"** for each data type individually. Paste the relevant block below.

### READ_HEART_RATE
> Cadence reads heart rate continuously (every ~3 minutes) to drive two core behaviours: (1) the current BPM is displayed on-screen and included in the AI prompt so the music generation pipeline can match musical tempo to the user's physiological intensity; (2) if heart rate drifts by ±10 bpm during playback, the app discards the current audio buffer and generates a new song with updated biometric context, keeping the music in sync with real-time effort.

### READ_HEART_RATE_VARIABILITY
> Cadence reads today's HRV (RMSSD, in ms) and a 14-day personal baseline. Deviation from baseline contributes ±15 points to the daily readiness score (higher HRV than usual raises readiness; lower HRV lowers it). HRV is one of the strongest physiological indicators of recovery and stress, and its contribution to readiness directly influences the generated music's tempo and intensity targets.

### READ_RESTING_HEART_RATE
> Cadence reads resting heart rate for today and maintains a 14-day personal baseline. The difference between today's reading and the baseline contributes ±15 points to the daily readiness score (a higher-than-usual resting HR lowers readiness; lower-than-usual raises it). This readiness score is included in the AI music prompt to guide tempo and energy selection appropriate to the user's recovery state.

### READ_SLEEP
> Cadence reads the most recent sleep session from the last 48 hours, including total duration and sleep stage breakdown (deep, REM, light, awake). From this it computes a 0–100 sleep quality score (40 pts for ≥7 hours; 30 pts each for ≥20% deep and ≥20% REM sleep). The score and stage percentages are included in the AI prompt and contribute ±25 points to the daily readiness score. A low sleep readiness score nudges generated music toward lower tempos and calmer moods; low REM specifically triggers a preference for simpler melodic structures.

### READ_OXYGEN_SATURATION
> Cadence reads the latest SpO2 reading (last 24 hours) and uses it as a safety gate in the music generation pipeline: if SpO2 is below 94%, the app overrides all other parameters and restricts generated music to ambient tracks below 60 BPM. This prevents energetic, high-tempo music from being generated when the biometric data indicates the user may be under physiological stress. The value is also included in the AI prompt context.

### READ_STEPS
> Cadence reads today's cumulative step count (refreshed every ~5 minutes) and includes it in the biometric context string sent to the AI music generation pipeline (e.g. "4 321 steps today"). Step count helps the model understand daily activity volume when inferring an appropriate musical energy level and tempo.

### READ_DISTANCE
> Cadence reads today's total distance travelled (in km, refreshed every ~5 minutes) and includes it in the AI music prompt context (e.g. "3.2 km today"). It provides additional activity-volume context alongside step count so the music generation model can better calibrate musical energy to match the user's day.

### READ_ACTIVE_CALORIES_BURNED
> Cadence reads today's active calories burned to enrich the biometric context sent to its AI music generation pipeline. The daily calorie total is included in a prompt string (e.g. "312 kcal today") alongside other metrics like heart rate and steps. Active calories are also compared against a 14-day personal baseline to calculate a "readiness load" score (−10 to 0 pts): sustained high-output days slightly lower the readiness score, nudging the generated music toward recovery-appropriate tempo and mood.

### READ_TOTAL_CALORIES_BURNED
> Cadence reads total calories burned as a fallback only — if the Health Connect source provides no active calorie data for today, the app queries total calories burned instead and uses it identically: as a daily-calorie figure in the AI music prompt context. This ensures the prompt remains complete even on devices or wearables that record calories under a different record type.

### READ_EXERCISE
> Cadence reads today's exercise session records to compute total active minutes. This figure is included in the AI prompt (e.g. "47 mins activity today") to give the model a sense of workout duration context, helping it distinguish a high-step/low-activity day from an intense short session.

### READ_FLOORS_CLIMBED
> Cadence reads today's floors climbed count and includes it in the AI music prompt context (e.g. "12 floors today") alongside steps, distance, and calories. It adds further texture to the daily activity picture used by the music generation model to calibrate tempo and energy to the user's physical output.

### READ_BLOOD_PRESSURE
> Cadence reads the most recent blood pressure reading (last 24 hours) and includes the systolic and diastolic values in the biometric context string sent to the AI music generation pipeline. The values are used as raw contextual metrics to give the LLM a more complete picture of cardiovascular state when inferring musical style parameters.

### READ_BODY_TEMPERATURE
> Cadence reads the most recent body temperature reading (last 24 hours, in °C) and includes it as a raw metric in the biometric context string sent to the AI music generation pipeline. It is one of several physiological signals that help the model understand the user's overall physical state when selecting music style parameters.
