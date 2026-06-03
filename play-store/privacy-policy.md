# Privacy Policy — Cadence AI Music

**Last updated:** 20 April 2026

This privacy policy describes how Cadence AI Music ("the app", "we", "us") handles information when you use it on your Android device.

## 1. Who we are

Cadence AI Music is developed by **[YOUR FULL LEGAL NAME]**, based in the United Kingdom.

- Contact: **[YOUR CONTACT EMAIL]**
- Website: https://cadencemusics.uk

## 2. What data the app uses

Cadence AI Music personalises instrumental music to your current physiological state. To do that, it uses the following categories of data:

### 2.1 Health and fitness data (via Android Health Connect)
- Heart rate (including resting heart rate and heart-rate variability)
- Sleep duration and stages (deep, REM)
- SpO₂ (blood oxygen)
- Step count, distance, and active calories
- Body temperature (if available)
- Blood pressure (if available)

You grant access to each of these separately via the standard Android Health Connect permission prompt. You can revoke any permission at any time from **Settings → Apps → Health Connect → App permissions**.

### 2.2 Location data
- Precise location (GPS coordinates and speed)

Used to detect your activity context (walking, running, commuting) and to look up current local weather. Location is not stored on any server.

### 2.3 App preferences
- Your genre toggles, energy bias, and free-text adjustments
- Anonymous song-rating feedback used to build an on-device taste profile

These preferences live only on your device in Android's private app storage.

## 3. How your data is processed

### 3.1 On-device processing
All raw sensor readings — heart rate, sleep, steps, GPS coordinates — are read and processed **locally on your device**. They are never uploaded in raw form.

### 3.2 Anonymous summaries sent off-device
To generate music matched to your state, the app sends a short **anonymised biometric summary** (e.g. "heart rate 92, walking, 18:30, overcast") to the music-generation backend. This summary:
- Contains no personal identifiers (no name, email, device ID, or account ID)
- Is ephemeral — it is processed in memory to produce a music style, then discarded
- Is transmitted over HTTPS (TLS 1.2+)

### 3.3 Third-party services the app contacts
| Service | What it receives | Why |
|---|---|---|
| **Cadence inference backend** (`chat.cadencemusics.uk`, `api.cadencemusics.uk`) | Anonymised biometric summary → song style parameters → audio | Music generation |
| **Open-Meteo** (`api.open-meteo.com`) | Approximate coordinates (rounded) | Current local weather |
| **Google Play Services** | Location fix request | Standard Android location API |
| **Android Health Connect** | Read requests for the health data types above | System health-data broker |

No analytics SDK, advertising SDK, or crash-reporting SDK is embedded in the app.

## 4. What we do *not* do

- We do not collect your name, email, phone number, or any account credential.
- We do not sell, rent, or share your data with advertisers.
- We do not build a profile that can be used to identify you.
- We do not store your health data on our servers.
- We do not track your location history.

## 5. Data retention

- Data sent to the inference backend is processed in memory and discarded at request end. Request logs retain only coarse statistics (timestamp, request ID, model version) for service reliability; these contain no biometric values.
- On-device preferences (taste memory, user adjustments) remain on your device until you uninstall the app or clear its data.

## 6. Your rights under UK GDPR

If you are in the UK or the EU, you have the right to:
- Ask what data we hold about you (answer: no personally identifiable data)
- Request deletion of any stored preferences (uninstall the app, or clear app data)
- Object to processing
- Lodge a complaint with the UK Information Commissioner's Office (https://ico.org.uk)

Because the app does not associate data with an identity, there is typically no record to access or delete beyond what lives on your own device.

## 7. Children

Cadence AI Music is not directed at children under 13. Health Connect itself requires Android 14+ and a Google account; users must meet Google's minimum age requirements.

## 8. Security

- All network traffic uses TLS 1.2 or newer.
- The inference backend is hosted on a private UK research network with restricted access.
- No persistent storage of biometric data occurs off your device.

## 9. Changes to this policy

We will post material changes on this page and update the "Last updated" date. Continued use of the app after a change constitutes acceptance.

## 10. Contact

Questions or data-rights requests: **[YOUR CONTACT EMAIL]**
