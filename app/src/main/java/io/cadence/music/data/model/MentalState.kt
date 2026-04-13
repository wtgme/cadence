package io.cadence.music.data.model

/**
 * User's current mental and physiological state as estimated by the LLM (Step 1a).
 * All numeric fields are nullable — partial parse is still usable for Step 1b.
 * Consider step 1a a failure only when both [arousal] and [valence] are null.
 */
data class MentalState(
    /** Russell circumplex activation axis. 0 = deeply relaxed, 10 = maximally activated. */
    val arousal: Int?,
    /** Russell circumplex pleasure axis. -5 = very distressed, 0 = neutral, +5 = very happy. */
    val valence: Int?,
    /** Psychological stress. 0 = completely relaxed, 10 = extreme stress. */
    val stress: Int?,
    /** Subjective physical energy. 0 = exhausted, 10 = fully energised. */
    val energy: Int?,
    /** Attentional focus. 0 = scattered/drowsy, 10 = deep sustained concentration. */
    val focus: Int?,
    /** Short descriptive phrase, e.g. "alert and motivated". */
    val mood: String?,
    /** Full LLM response text, preserved for debug display. */
    val rawLlmText: String,
)
