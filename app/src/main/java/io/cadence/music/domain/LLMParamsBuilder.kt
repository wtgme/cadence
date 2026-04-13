package io.cadence.music.domain

import io.cadence.music.data.api.GenerationRepository
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds [SongParams] by calling OpenRouter once per session.
 *
 * Translates the current biometric + scene snapshot into lyrics and style tags via
 * [GenerationRepository.translateMetrics]. Falls back to rule-based params automatically
 * if OpenRouter is unavailable (handled inside [translateMetrics]).
 *
 * This is called only once per playback session — [AudioBufferManager] caches the result
 * in [sessionParams] and reuses it for every subsequent song until the user stops and
 * starts again.
 */
@Singleton
class LLMParamsBuilder @Inject constructor(
    private val musicRepository: GenerationRepository,
    private val promptBuilder: PromptBuilder,
) : ParamsBuilder {

    override suspend fun buildParams(state: SensorState, scene: Scene?): SongParams {
        val metricsContext = promptBuilder.buildMetricsContext(state, scene)
        return musicRepository.translateMetrics(metricsContext)
    }
}
