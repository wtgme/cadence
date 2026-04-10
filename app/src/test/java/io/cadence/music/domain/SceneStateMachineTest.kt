package io.cadence.music.domain

import app.cash.turbine.test
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SceneStateMachineTest {

    private fun makeMachine(testDispatcher: kotlinx.coroutines.test.TestCoroutineScheduler? = null): SceneStateMachine {
        val dispatcher = if (testDispatcher != null) StandardTestDispatcher(testDispatcher) else StandardTestDispatcher()
        return SceneStateMachine(SceneDetector(), dispatcher)
    }

    private fun runningState() = SensorState(speedKmh = 10f, heartRate = 150)
    private fun restingState() = SensorState(speedKmh = 0f, heartRate = 60)

    @Test
    fun `same candidate repeated does not reset the debounce timer`() = runTest {
        val machine = makeMachine(testScheduler)
        machine.confirmedScene.test {
            machine.process(runningState())
            advanceTimeBy(5_000)
            machine.process(runningState())
            advanceTimeBy(5_000)
            machine.process(runningState())
            // 10s elapsed since first process; with bug fixed, timer started at 0 and fires at 15s
            expectNoEvents()
            advanceTimeBy(5_001)
            assertEquals(Scene.RUNNING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `different candidate resets the debounce timer`() = runTest {
        val machine = makeMachine(testScheduler)
        machine.confirmedScene.test {
            machine.process(runningState())
            advanceTimeBy(10_000)
            machine.process(restingState())  // reset — new 15s window starts here
            advanceTimeBy(10_000)
            expectNoEvents()
            advanceTimeBy(5_001)
            assertEquals(Scene.RESTING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `scene is emitted after debounce period`() = runTest {
        val machine = makeMachine(testScheduler)
        machine.confirmedScene.test {
            machine.process(runningState())
            advanceTimeBy(14_999)
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(Scene.RUNNING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `forceScene emits immediately without waiting for debounce`() = runTest {
        val machine = makeMachine(testScheduler)
        machine.confirmedScene.test {
            machine.forceScene(Scene.COMMUTING)
            assertEquals(Scene.COMMUTING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returning to lastEmittedScene cancels pending timer`() = runTest {
        val machine = makeMachine(testScheduler)
        machine.confirmedScene.test {
            // First confirm RUNNING
            machine.process(runningState())
            advanceTimeBy(15_001)
            assertEquals(Scene.RUNNING, awaitItem())

            // Start resting timer
            machine.process(restingState())
            advanceTimeBy(5_000)

            // Return to running (the confirmed scene) — should cancel resting timer
            machine.process(runningState())
            advanceTimeBy(20_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
