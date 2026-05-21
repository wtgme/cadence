package io.cadence.music.data.model

/**
 * On-device classification of the user's current physical activity, sourced from
 * Google Play Services' ActivityRecognitionClient. Mirrors the iOS `MotionActivity`
 * enum (CoreMotion).
 *
 * Only high-confidence classifications should be treated as authoritative; lower
 * confidence values should fall back to GPS+HR heuristics in `SceneDetector`.
 */
enum class MotionActivity {
    STATIONARY,
    WALKING,
    RUNNING,
    CYCLING,
    AUTOMOTIVE,
    UNKNOWN,
}
