package io.cadence.music.data.model

/**
 * Activity type of an in-progress or just-finished workout reported by the user's
 * watch via Health Connect's [androidx.health.connect.client.records.ExerciseSessionRecord].
 * When present, this is the highest-confidence signal for scene classification — it
 * originates from a wrist sensor that the OS has already classified.
 *
 * Mirrors the iOS `ActiveWorkoutType` enum (HealthKit).
 */
enum class ActiveWorkoutType {
    RUNNING,
    CYCLING,
    WALKING,
    ROWING,
    ELLIPTICAL,
    HIIT,
    OTHER,
}
