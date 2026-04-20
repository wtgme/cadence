package io.cadence.music.audio

import io.cadence.music.data.adjustment.UserAdjustmentRepository
import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.api.StreamingChunk
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import io.cadence.music.data.session.CachedSessionParams
import io.cadence.music.data.session.LastSessionParamsStore
import io.cadence.music.domain.ParamsBuilder
import io.cadence.music.domain.PromptBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AudioBufferManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val fakeSongParams = SongParams(lyric = ".", descriptions = "electronic,energetic")
    private val fakeMentalState = MentalState(
        arousal = 5, valence = 0, stress = 3, energy = 5, focus = 5,
        mood = "neutral", rawLlmText = "",
    )

    private fun fakeFile(name: String): File = tmpFolder.newFile(name).also { it.writeText("x") }

    /**
     * streamBlock is last so callers can use trailing lambda syntax.
     * [builderFactory] gets the mock's mutable mental-state flow so custom builders can
     * populate it — mirroring production where [ParamsBuilder.buildParams] triggers
     * [GenerationRepository.translateMetrics] which sets [GenerationRepository.translatedMentalState].
     */
    private fun makeManager(
        builderFactory: ((MutableStateFlow<MentalState?>) -> ParamsBuilder)? = null,
        streamBlock: (Int) -> Flow<StreamingChunk>,
    ): AudioBufferManager {
        var callCount = 0
        val mentalStateFlow = MutableStateFlow<MentalState?>(null)
        val repo = object : GenerationRepository {
            override val translatedSongParams = MutableStateFlow<SongParams?>(null)
            override val translatedMentalState: MutableStateFlow<MentalState?> = mentalStateFlow
            override suspend fun translateMetrics(ctx: String) = fakeSongParams
            override suspend fun translateMentalState(mentalState: MentalState, previousParams: SongParams?) = fakeSongParams
            override fun generateAudioStream(params: SongParams): Flow<StreamingChunk> =
                streamBlock(++callCount)
        }
        val builder = builderFactory?.invoke(mentalStateFlow) ?: object : ParamsBuilder {
            override suspend fun buildParams(state: SensorState, scene: Scene?): SongParams {
                mentalStateFlow.value = fakeMentalState
                return fakeSongParams
            }
        }
        return AudioBufferManager(
            musicRepository = repo,
            paramsBuilder = builder,
            promptBuilder = PromptBuilder(),
            userAdjustmentRepository = UserAdjustmentRepository(),
            lastSessionParams = NoOpLastSessionParamsStore,
            audioCacheDir = tmpFolder.newFolder("audio_cache"),
        )
    }

    private object NoOpLastSessionParamsStore : LastSessionParamsStore {
        override suspend fun load(): CachedSessionParams? = null
        override suspend fun save(
            params: SongParams,
            mentalState: MentalState?,
            scene: Scene?,
            heartRate: Int,
        ) {}
        override fun isFreshFor(
            cached: CachedSessionParams,
            currentScene: Scene?,
            currentHr: Int,
        ): Boolean = false
    }

    /** Emits N Audio chunks then Complete. */
    private fun chunkStream(n: Int): Flow<StreamingChunk> = flow {
        repeat(n) { i ->
            emit(StreamingChunk.Audio(fakeFile("chunk_${i}_${System.nanoTime()}.mp3"), i, fakeSongParams))
        }
        emit(StreamingChunk.Complete)
    }

    @Test
    fun `prime enqueues chunks from stream`(): Unit = runBlocking {
        val manager = makeManager { _ -> chunkStream(2) }
        manager.prime(SensorState(), Scene.RESTING)
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 2 }
        }
        assertTrue("Expected at least 2 chunks", manager.chunksReady.value >= 2)
    }

    @Test
    fun `drainAndReprime clears error state`(): Unit = runBlocking {
        val fail = AtomicBoolean(true)
        val manager = makeManager { _ ->
            if (fail.get()) flowOf(StreamingChunk.Error("down")) else chunkStream(1)
        }
        manager.prime(SensorState(), Scene.RESTING)
        withTimeout(3_000) {
            manager.lastError.first { it != null }
        }
        assertNotNull(manager.lastError.value)

        fail.set(false)
        manager.drainAndReprime(SensorState(), Scene.RUNNING)
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 1 }
        }
        assertNull(manager.lastError.value)
        assertTrue("Expected at least 1 chunk after reprime", manager.chunksReady.value >= 1)
    }

    @Test
    fun `stale generations from old epoch are discarded after drainAndReprime`(): Unit = runBlocking {
        val manager = makeManager { _ ->
            flow {
                kotlinx.coroutines.delay(100) // simulate slow generation
                emit(StreamingChunk.Audio(fakeFile("clip_${System.nanoTime()}.mp3"), 0, fakeSongParams))
                emit(StreamingChunk.Complete)
            }
        }
        manager.prime(SensorState(), Scene.RESTING)
        // Drain immediately before the in-flight stream finishes
        manager.drainAndReprime(SensorState(), Scene.RUNNING)
        // Wait for the new epoch's stream to deliver at least one chunk
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 1 }
        }
        assertTrue("Expected chunks from new epoch", manager.chunksReady.value >= 1)
    }

    @Test
    fun `retryGeneration clears error and resumes generation`(): Unit = runBlocking {
        val fail = AtomicBoolean(true)
        val manager = makeManager { _ ->
            if (fail.get()) flowOf(StreamingChunk.Error("err")) else chunkStream(1)
        }
        manager.prime(SensorState(), Scene.RESTING)
        withTimeout(3_000) {
            manager.lastError.first { it != null }
        }
        assertNotNull(manager.lastError.value)

        fail.set(false)
        manager.retryGeneration()
        withTimeout(5_000) {
            manager.lastError.first { it == null }
        }
        assertNull(manager.lastError.value)
    }

    @Test
    fun `drainAndReprime reuses session params without re-querying Signal2Style`(): Unit = runBlocking {
        val buildCount = AtomicInteger(0)
        val manager = makeManager(
            builderFactory = { mentalStateFlow ->
                object : ParamsBuilder {
                    override suspend fun buildParams(state: SensorState, scene: Scene?): SongParams {
                        buildCount.incrementAndGet()
                        mentalStateFlow.value = fakeMentalState
                        return fakeSongParams
                    }
                }
            },
        ) { _ -> chunkStream(1) }

        // Start session — first call should query Signal2Style (buildParams)
        manager.prime(SensorState(), Scene.RESTING)
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 1 }
        }
        assertEquals("Expected exactly 1 Signal2Style call after prime", 1, buildCount.get())

        // Mid-session context shift — should NOT re-query Signal2Style
        manager.drainAndReprime(SensorState(), Scene.RUNNING)
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 1 }
        }
        assertEquals("drainAndReprime must not re-query Signal2Style", 1, buildCount.get())

        // Stop + start again — should re-query Signal2Style with fresh biometrics
        manager.prime(SensorState(), Scene.WALKING)
        withTimeout(5_000) {
            manager.chunksReady.first { it >= 2 }
        }
        assertEquals("Second prime must trigger a new Signal2Style call", 2, buildCount.get())
    }
}
