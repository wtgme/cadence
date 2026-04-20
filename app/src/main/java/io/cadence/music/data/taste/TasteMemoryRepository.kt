package io.cadence.music.data.taste

import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.UserTasteMemory
import kotlinx.coroutines.flow.StateFlow

/**
 * Records and surfaces user taste signals to personalise music generation prompts.
 *
 * Feedback signals range from -1.0 (strong skip) to +1.0 (full listen):
 *   +1.0 = completed song (listened ≥90%)
 *   +0.5 = partial listen (50–89%)
 *   -0.5 = skipped mid-song (10–49%)
 *   -1.0 = skipped early (<10%)
 *
 * Use [buildTasteContext] to get a concise string ready for injection into
 * the Signal2Style Step 1b prompt (mental state → song params).
 */
interface TasteMemoryRepository {

    /** Live taste profile; updates after every [recordFeedback] call. */
    val tasteMemory: StateFlow<UserTasteMemory>

    /**
     * Records a feedback signal for [params] played under [scene].
     * [signal] is clamped to [-1.0, +1.0].
     */
    suspend fun recordFeedback(params: SongParams, scene: Scene?, signal: Float)

    /**
     * Builds a concise taste context string for LLM prompt injection.
     * Returns an empty string when there are fewer than [MIN_FEEDBACK_FOR_CONTEXT]
     * signals — avoids injecting noise before the profile is meaningful.
     */
    fun buildTasteContext(): String

    /** Wipes all accumulated taste data from memory and DataStore. */
    suspend fun reset()

    companion object {
        /** Minimum feedback signals before taste context is surfaced to the LLM. */
        const val MIN_FEEDBACK_FOR_CONTEXT = 3
    }
}
