package io.cadence.music.data.model

enum class Scene {
    STUCK_IN_TRAFFIC,
    COMMUTING,
    RUNNING,
    WALKING,
    RESTING;

    fun displayName(): String = when (this) {
        STUCK_IN_TRAFFIC -> "Stuck in Traffic"
        COMMUTING -> "Driving"
        RUNNING -> "Running"
        WALKING -> "Walking"
        RESTING -> "Resting"
    }
}
