package io.cadence.music.audio

import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.GenerationResult
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.domain.PromptBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AudioBufferManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val fakeSongParams = SongParams(lyric = "Test lyric", tags = "test")

    private fun fakeFile(name: String): File = tmpFolder.newFile(name).also { it.writeText("x") }

    private fun makeManager(generateBlock: suspend (Int) -> GenerationResult): AudioBufferManager {
        val callCount = AtomicInteger(0)
        val repo = object : GenerationRepository {
            override suspend fun generateClip(metricsContext: String) =
                generateBlock(callCount.incrementAndGet())
        }
        return AudioBufferManager(musicRepository = repo, promptBuilder = PromptBuilder())
    }

    @Test
    fun `prime enqueues 2 chunks`() = runTest {
        val manager = makeManager { n ->
            GenerationResult.Success(fakeFile("clip$n.mp3"), fakeSongParams)
        }
        manager.prime(SensorState(), Scene.RESTING)
        delay(200)
        assertEquals(2, manager.chunksReady.value)
    }

    @Test
    fun `drainAndReprime clears error state`() = runTest {
        val fail = AtomicBoolean(true)
        val manager = makeManager { _ ->
            if (fail.get()) GenerationResult.Error("down") else GenerationResult.Success(fakeFile("ok.mp3"), fakeSongParams)
        }
        manager.prime(SensorState(), Scene.RESTING)
        delay(300)
        assertNotNull(manager.lastError.value)

        fail.set(false)
        manager.drainAndReprime(SensorState(), Scene.RUNNING)
        delay(200)
        assertNull(manager.lastError.value)
        assertEquals(2, manager.chunksReady.value)
    }

    @Test
    fun `stale generations from old epoch are discarded after drainAndReprime`() = runTest {
        val manager = makeManager { _ ->
            delay(80) // simulate latency so old coroutines are still in-flight
            GenerationResult.Success(fakeFile("clip_${System.nanoTime()}.mp3"), fakeSongParams)
        }
        manager.prime(SensorState(), Scene.RESTING)
        // Immediately drain before old coroutines finish
        manager.drainAndReprime(SensorState(), Scene.RUNNING)
        delay(400)
        // Only the 2 new-epoch chunks should have been counted
        assertEquals(2, manager.chunksReady.value)
    }

    @Test
    fun `retryGeneration clears error and enqueues one more chunk`() = runTest {
        val fail = AtomicBoolean(true)
        val manager = makeManager { _ ->
            if (fail.get()) GenerationResult.Error("err") else GenerationResult.Success(fakeFile("r.mp3"), fakeSongParams)
        }
        manager.prime(SensorState(), Scene.RESTING)
        delay(300)
        val countBefore = manager.chunksReady.value
        assertNotNull(manager.lastError.value)

        fail.set(false)
        manager.retryGeneration()
        delay(100)
        assertNull(manager.lastError.value)
        assertEquals(countBefore + 1, manager.chunksReady.value)
    }
}
