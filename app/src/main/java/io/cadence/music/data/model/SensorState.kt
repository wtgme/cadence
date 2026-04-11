package io.cadence.music.data.model

data class SensorState(
    val speedKmh: Float = 0f,
    val heartRate: Int = 0,
    val hourOfDay: Int = 0,
    val weather: String = "Clear",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val sleepScore: Int = 0,           // 0 = no data; 1-100 = real computed value
    val sleepHours: Float = 0f,
    val sleepDeepPct: Float = 0f,
    val sleepRemPct: Float = 0f,
    val activityMinutesToday: Int = 0,  // exercise session minutes today
    val caloriesBurned: Float = 0f,     // kcal today
    val stepsToday: Long = 0,
    val distanceKm: Float = 0f,         // km today
    val spo2: Int = 0,
    val bloodPressureSystolic: Int = 0,
    val bloodPressureDiastolic: Int = 0,
    val bodyTemperature: Float = 0f,
    val floorsClimbed: Int = 0,
    // Readiness
    val readinessScore: Int = 0,           // 0 = unknown; 1..100 otherwise
    val readinessBreakdown: String = "",   // e.g. "Sleep +18, HRV +5, RHR -2"
)
