# Data Safety Form — Answers for Cadence AI Music

The Play Console Data Safety form asks a structured set of questions. Below are the answers for each section, tuned to Cadence AI Music's actual data flow.

---

## Section 1: Data collection and security

**Does your app collect or share any of the required user data types?**
→ **Yes**

**Is all of the user data collected by your app encrypted in transit?**
→ **Yes** (all network traffic uses HTTPS / TLS 1.2+)

**Do you provide a way for users to request that their data be deleted?**
→ **Yes — users can uninstall the app or clear app data from Android Settings, which removes all locally-stored data. No off-device user data is retained.**

---

## Section 2: Data types — what you collect and share

For each data type, answer these sub-questions:
- **Collected?** Yes/No (sent off-device)
- **Shared?** Yes/No (transferred to another company)
- **Processing type:** Ephemeral (not stored) or Persistent
- **Required or Optional**
- **Purposes**

### Location
| Question | Answer |
|---|---|
| Precise location | **Collected** |
| Shared? | **Yes** — with Open-Meteo (weather service) |
| Ephemeral? | **Yes** — not stored |
| Required / Optional? | **Required** for scene detection; user must grant permission |
| Purposes | App functionality, Analytics → tick only *App functionality* |

### Personal info
- **Name, email, user ID, address, phone, race, political/religious beliefs, sexual orientation, other personal info**
- → **Not collected, not shared** (none of these)

### Financial info
- → **Not collected, not shared**

### Health and fitness
| Data type | Collected? | Shared? | Purpose |
|---|---|---|---|
| Health info (heart rate, HRV, SpO₂, sleep, blood pressure, body temperature) | **Yes — ephemeral, anonymised summary only** | **No** | App functionality |
| Fitness info (steps, distance, active calories, exercise) | **Yes — ephemeral, anonymised summary only** | **No** | App functionality |

Processing type for both: **Ephemeral** (processed in memory, not stored on any server).
Required: **Required** (the app's core function depends on it; user must grant Health Connect permissions).

### Messages
- → **Not collected, not shared**

### Photos and videos
- → **Not collected, not shared**

### Audio files
- → **Not collected, not shared** (music is generated, not uploaded)

### Files and docs
- → **Not collected, not shared**

### Calendar
- → **Not collected, not shared**

### Contacts
- → **Not collected, not shared**

### App activity
| Data type | Collected? | Shared? | Purpose |
|---|---|---|---|
| App interactions (song ratings, manual adjustments) | **No — stored only on-device** | **No** | — |
| In-app search history | **Not collected** | — | — |
| Installed apps | **Not collected** | — | — |
| Other user-generated content (taste profile, preferences) | **No — stored only on-device** | **No** | — |

Rationale: taste memory and user-adjustment preferences are persisted in Android DataStore (private app storage). They never leave the device.

### Web browsing
- → **Not collected, not shared**

### App info and performance
| Data type | Collected? | Shared? |
|---|---|---|
| Crash logs | **Not collected** | — |
| Diagnostics | **Not collected** | — |
| Other app performance data | **Not collected** | — |

(There is no Firebase / Crashlytics / Sentry / other analytics SDK in the build.)

### Device or other IDs
- **Device or other IDs (AAID, Android ID, IMEI, MAC address)** → **Not collected**

---

## Section 3: Summary table (what Play Console will generate)

Data the app collects:
- **Location** (precise) — shared with Open-Meteo, ephemeral, required for app functionality
- **Health info** — not shared, ephemeral, required for app functionality
- **Fitness info** — not shared, ephemeral, required for app functionality

Data the app does NOT collect:
- Personal info
- Financial info
- Messages, photos, audio, files, calendar, contacts
- App interactions / search / installed apps (kept on-device)
- Device IDs
- Crash or diagnostic data

---

## Key justification phrases (copy into form text boxes)

If Play Console asks *why* a data type is collected or *what app functionality* it powers, use these:

### Heart rate / HRV / sleep / SpO₂ / steps
> "The app reads these values from Android Health Connect to estimate the user's current physiological state and generate music that matches that state. Only an anonymised summary of the current values (no identifiers, no history) is transmitted over HTTPS to the music-generation endpoint, where it is processed in memory and discarded at request end. No health data is persisted on any server."

### Precise location
> "GPS is used to detect the user's activity context via speed (walking, running, commuting) and to look up current local weather from the Open-Meteo public weather API. Coordinates are never stored. The weather query sends coordinates to Open-Meteo only for the duration of one request."

### Why "ephemeral" is accurate
> "The backend generates music in response to each request and does not retain biometric summaries between requests. Request-level service logs contain only a timestamp, a random request ID, and the model version — no biometric values, no identifiers."
