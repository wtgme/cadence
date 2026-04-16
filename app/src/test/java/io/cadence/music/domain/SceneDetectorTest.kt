package io.cadence.music.domain

import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SceneDetectorTest {

    private lateinit var detector: SceneDetector

    @Before
    fun setUp() {
        detector = SceneDetector()
    }

    private fun stateWith(speedKmh: Float = 0f, heartRate: Int = 60, hourOfDay: Int = 10, dayOfWeek: Int = 3) =
        SensorState(speedKmh = speedKmh, heartRate = heartRate, hourOfDay = hourOfDay, dayOfWeek = dayOfWeek)

    // ── Commuting ──────────────────────────────────────────────────────────────

    @Test
    fun `speed at or above commuting threshold returns COMMUTING`() {
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = 25f)))
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = 60f)))
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = SceneDetector.COMMUTING_SPEED_THRESHOLD)))
    }

    // ── Running ────────────────────────────────────────────────────────────────

    @Test
    fun `speed in running range returns RUNNING`() {
        assertEquals(Scene.RUNNING, detector.detect(stateWith(speedKmh = 8f)))
        assertEquals(Scene.RUNNING, detector.detect(stateWith(speedKmh = 15f)))
    }

    @Test
    fun `high HR alone returns RUNNING`() {
        assertEquals(Scene.RUNNING, detector.detect(stateWith(speedKmh = 0f, heartRate = 136)))
        assertEquals(Scene.RUNNING, detector.detect(stateWith(speedKmh = 0f, heartRate = 180)))
    }

    @Test
    fun `HR at threshold boundary does not trigger RUNNING`() {
        // HR exactly at 135 is NOT > 135, so falls to WORKOUT (135 > 100)
        assertEquals(Scene.WORKOUT, detector.detect(stateWith(speedKmh = 0f, heartRate = SceneDetector.RUNNING_HR_THRESHOLD, hourOfDay = 10)))
    }

    // ── Cycling ────────────────────────────────────────────────────────────────

    @Test
    fun `cycling speed with normal HR returns CYCLING`() {
        assertEquals(Scene.CYCLING, detector.detect(stateWith(speedKmh = 4f, heartRate = 90)))
        assertEquals(Scene.CYCLING, detector.detect(stateWith(speedKmh = 7f, heartRate = 80)))
    }

    // ── Walking ────────────────────────────────────────────────────────────────

    @Test
    fun `walking speed returns WALKING`() {
        assertEquals(Scene.WALKING, detector.detect(stateWith(speedKmh = 2f)))
        assertEquals(Scene.WALKING, detector.detect(stateWith(speedKmh = 3f)))
    }

    // ── Workout ────────────────────────────────────────────────────────────────

    @Test
    fun `stationary with elevated HR returns WORKOUT`() {
        assertEquals(Scene.WORKOUT, detector.detect(stateWith(speedKmh = 0f, heartRate = 101)))
        assertEquals(Scene.WORKOUT, detector.detect(stateWith(speedKmh = 0f, heartRate = 130)))
    }

    @Test
    fun `HR at workout threshold boundary does not trigger WORKOUT`() {
        // Exactly at threshold is not above it — should fall through to FOCUS or RESTING
        assertEquals(Scene.FOCUS, detector.detect(stateWith(speedKmh = 0f, heartRate = SceneDetector.WORKOUT_HR_THRESHOLD, hourOfDay = 10)))
    }

    // ── Focus ──────────────────────────────────────────────────────────────────

    @Test
    fun `stationary normal HR during daytime returns FOCUS`() {
        assertEquals(Scene.FOCUS, detector.detect(stateWith(speedKmh = 0f, heartRate = 65, hourOfDay = 9)))
        assertEquals(Scene.FOCUS, detector.detect(stateWith(speedKmh = 0f, heartRate = 65, hourOfDay = 14)))
        assertEquals(Scene.FOCUS, detector.detect(stateWith(speedKmh = 0f, heartRate = 65, hourOfDay = 18)))
    }

    // ── Party ──────────────────────────────────────────────────────────────────

    @Test
    fun `weekend evening with elevated HR returns PARTY`() {
        // Saturday 22:00, HR 85
        assertEquals(Scene.PARTY, detector.detect(stateWith(heartRate = 85, hourOfDay = 22, dayOfWeek = 7)))
        // Friday 21:00, HR 80
        assertEquals(Scene.PARTY, detector.detect(stateWith(heartRate = 80, hourOfDay = 21, dayOfWeek = 6)))
        // Sunday 23:00, HR 90
        assertEquals(Scene.PARTY, detector.detect(stateWith(heartRate = 90, hourOfDay = 23, dayOfWeek = 1)))
    }

    @Test
    fun `weekday night with strong HR returns PARTY`() {
        // Tuesday 22:00, HR 95 (above strong threshold)
        assertEquals(Scene.PARTY, detector.detect(stateWith(heartRate = 95, hourOfDay = 22, dayOfWeek = 3)))
    }

    @Test
    fun `weekday night with low HR does not return PARTY`() {
        // Tuesday 22:00, HR 65 — not enough for party
        assertEquals(Scene.RESTING, detector.detect(stateWith(heartRate = 65, hourOfDay = 22, dayOfWeek = 3)))
    }

    @Test
    fun `daytime does not return PARTY even on weekend`() {
        // Saturday 14:00, HR 85 — daytime, should be Focus not Party
        assertEquals(Scene.FOCUS, detector.detect(stateWith(heartRate = 85, hourOfDay = 14, dayOfWeek = 7)))
    }

    @Test
    fun `high speed at night returns COMMUTING not PARTY`() {
        // Speed-based scenes take priority
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = 30f, heartRate = 85, hourOfDay = 22, dayOfWeek = 7)))
    }

    // ── Resting ────────────────────────────────────────────────────────────────

    @Test
    fun `stationary normal HR outside focus hours returns RESTING`() {
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0f, heartRate = 60, hourOfDay = 22)))
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0f, heartRate = 60, hourOfDay = 3)))
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0f, heartRate = 60, hourOfDay = 19)))
    }
}
