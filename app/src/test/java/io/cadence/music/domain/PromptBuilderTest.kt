package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PromptBuilderTest {

    private lateinit var builder: PromptBuilder

    @Before
    fun setUp() {
        builder = PromptBuilder()
    }

    private fun build(
        scene: Scene? = null,
        speedKmh: Float = 0f,
        heartRate: Int = 70,
        sleepScore: Int = 75,
        stepsToday: Long = 0,
        activityMinutes: Int = 0,
        caloriesBurned: Float = 0f,
        hourOfDay: Int = 10,
        weather: String = "Clear",
    ) = builder.buildMetricsContext(
        SensorState(
            speedKmh = speedKmh,
            heartRate = heartRate,
            sleepScore = sleepScore,
            stepsToday = stepsToday,
            activityMinutesToday = activityMinutes,
            caloriesBurned = caloriesBurned,
            hourOfDay = hourOfDay,
            weather = weather,
        ),
        scene
    )

    @Test
    fun `scene labels are correct`() {
        assertTrue(build(Scene.RUNNING).contains("Running"))
        assertTrue(build(Scene.CYCLING).contains("Cycling"))
        assertTrue(build(Scene.WALKING).contains("Walking"))
        assertTrue(build(Scene.COMMUTING).contains("Travelling"))
        assertTrue(build(Scene.WORKOUT).contains("Working Out"))
        assertTrue(build(Scene.FOCUS).contains("Focus"))
        assertTrue(build(Scene.PARTY).contains("Party"))
        assertTrue(build(Scene.RESTING).contains("Resting"))
        assertTrue(build(null).contains("Stationary"))
    }

    @Test
    fun `sleep score brackets produce correct labels`() {
        assertTrue(build(sleepScore = 80).contains("Well-rested"))
        assertTrue(build(sleepScore = 50).contains("Average sleep"))
        assertTrue(build(sleepScore = 30).contains("Poorly rested"))
    }

    @Test
    fun `zero heart rate shows unknown`() {
        assertTrue(build(heartRate = 0).contains("unknown"))
    }

    @Test
    fun `non-zero heart rate shows bpm value`() {
        val result = build(heartRate = 72)
        assertTrue(result.contains("72 bpm"))
        assertFalse(result.contains("unknown"))
    }

    @Test
    fun `time of day labels are correct`() {
        assertTrue(build(hourOfDay = 6).contains("Early morning"))
        assertTrue(build(hourOfDay = 10).contains("Morning"))
        assertTrue(build(hourOfDay = 12).contains("Midday"))
        assertTrue(build(hourOfDay = 15).contains("Afternoon"))
        assertTrue(build(hourOfDay = 19).contains("Evening"))
        assertTrue(build(hourOfDay = 23).contains("Night"))
    }

    @Test
    fun `output contains all expected fields`() {
        val result = build(
            scene = Scene.RUNNING,
            speedKmh = 10f,
            heartRate = 150,
            sleepScore = 80,
            stepsToday = 5000,
            activityMinutes = 30,
            caloriesBurned = 200f,
            weather = "Rainy",
        )
        assertTrue(result.contains("Activity:"))
        assertTrue(result.contains("GPS Speed:"))
        assertTrue(result.contains("Weather:") && result.contains("rainy"))
        assertTrue(result.contains("HR:"))
        assertTrue(result.contains("Sleep:"))
        assertTrue(result.contains("Time:"))
        assertTrue(result.contains("5000 steps"))
        assertTrue(result.contains("30 mins"))
        assertTrue(result.contains("200 kcal"))
    }
}
