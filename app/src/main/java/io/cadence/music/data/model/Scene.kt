package io.cadence.music.data.model

enum class Scene {
    RUNNING,
    CYCLING,
    WALKING,
    COMMUTING,
    WORKOUT,
    FOCUS,
    RESTING;

    fun displayName(): String = when (this) {
        RUNNING    -> "Running"
        CYCLING    -> "Cycling"
        WALKING    -> "Walking"
        COMMUTING  -> "Commuting"
        WORKOUT    -> "Working Out"
        FOCUS      -> "Focus"
        RESTING    -> "Resting"
    }
}
