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

    private fun stateWith(speedKmh: Float = 0f, heartRate: Int = 60) =
        SensorState(speedKmh = speedKmh, heartRate = heartRate)

    @Test
    fun `speed above driving threshold returns COMMUTING`() {
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = 25f)))
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = 60f)))
    }

    @Test
    fun `speed at driving threshold returns COMMUTING`() {
        assertEquals(Scene.COMMUTING, detector.detect(stateWith(speedKmh = SceneDetector.DRIVING_SPEED_THRESHOLD)))
    }

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
    fun `speed in walking range returns WALKING`() {
        assertEquals(Scene.WALKING, detector.detect(stateWith(speedKmh = 3f)))
        assertEquals(Scene.WALKING, detector.detect(stateWith(speedKmh = 5f)))
    }

    @Test
    fun `slow movement below walking threshold returns STUCK_IN_TRAFFIC`() {
        assertEquals(Scene.STUCK_IN_TRAFFIC, detector.detect(stateWith(speedKmh = 1.5f)))
        assertEquals(Scene.STUCK_IN_TRAFFIC, detector.detect(stateWith(speedKmh = 2f)))
    }

    @Test
    fun `stationary returns RESTING`() {
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0f)))
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0.5f)))
    }

    @Test
    fun `HR at threshold boundary does not trigger RUNNING`() {
        // Exactly at threshold — speed is zero, HR is exactly the threshold (not above)
        assertEquals(Scene.RESTING, detector.detect(stateWith(speedKmh = 0f, heartRate = SceneDetector.RUNNING_HR_THRESHOLD)))
    }
}
