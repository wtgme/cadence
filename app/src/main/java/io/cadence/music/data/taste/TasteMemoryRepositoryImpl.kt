package io.cadence.music.data.taste

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.UserTasteMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val Context.tasteDataStore by preferencesDataStore(name = "taste_memory")
private val TASTE_KEY = stringPreferencesKey("taste_memory_v1")

/**
 * DataStore-backed implementation of [TasteMemoryRepository].
 *
 * Scores are updated with EMA: newScore = oldScore × (1 − α) + signal × α
 * α = [ALPHA] is set so ~8 feedback signals bring a score near steady state.
 *
 * The full profile is serialised to a single JSON string in DataStore, keeping
 * I/O minimal (one small write per feedback event, ~300–500 bytes typical).
 */
@Singleton
class TasteMemoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) : TasteMemoryRepository {

    private val adapter = moshi.adapter(UserTasteMemory::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasteMemory = MutableStateFlow(UserTasteMemory())
    override val tasteMemory: StateFlow<UserTasteMemory> = _tasteMemory

    init {
        scope.launch { loadFromDataStore() }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private suspend fun loadFromDataStore() {
        val json = context.tasteDataStore.data
            .map { it[TASTE_KEY] }
            .firstOrNull() ?: return
        runCatching { adapter.fromJson(json) }
            .getOrNull()
            ?.let {
                _tasteMemory.value = it
                Log.d(TAG, "Loaded taste memory: feedbackCount=${it.feedbackCount}")
            }
    }

    private suspend fun persist(memory: UserTasteMemory) {
        runCatching {
            context.tasteDataStore.edit { prefs ->
                prefs[TASTE_KEY] = adapter.toJson(memory)
            }
        }.onFailure { Log.e(TAG, "Failed to persist taste memory: ${it.message}") }
    }

    // ── Core update ───────────────────────────────────────────────────────────

    override suspend fun recordFeedback(params: SongParams, scene: Scene?, signal: Float) {
        val clampedSignal = signal.coerceIn(-1f, 1f)
        val tags = params.descriptions
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            ?: return

        val genres = tags.filter { it in KNOWN_GENRES }

        withContext(Dispatchers.IO) {
            val current = _tasteMemory.value
            val newGenreScores   = current.genreScores.toMutableMap()
            val newTagScores     = current.tagScores.toMutableMap()
            val newContextScores = current.contextGenreScores.toMutableMap()

            for (genre in genres) {
                newGenreScores[genre] = ema(current.genreScores[genre] ?: 0f, clampedSignal)
            }
            for (tag in tags) {
                newTagScores[tag] = ema(current.tagScores[tag] ?: 0f, clampedSignal)
            }
            if (scene != null) {
                for (genre in genres) {
                    val key = "${scene.name}:$genre"
                    newContextScores[key] = ema(current.contextGenreScores[key] ?: 0f, clampedSignal)
                }
            }

            val updated = current.copy(
                genreScores        = newGenreScores,
                tagScores          = newTagScores,
                contextGenreScores = newContextScores,
                feedbackCount      = current.feedbackCount + 1,
                lastUpdatedMs      = System.currentTimeMillis(),
            )
            _tasteMemory.value = updated
            persist(updated)
            Log.d(TAG, "Taste feedback: signal=${"%.2f".format(clampedSignal)} scene=${scene?.name} tags=$tags count=${updated.feedbackCount}")
        }
    }

    // ── Prompt context ────────────────────────────────────────────────────────

    override fun buildTasteContext(): String =
        buildTasteContextFrom(_tasteMemory.value)

    // ── Reset ─────────────────────────────────────────────────────────────────

    override suspend fun reset() {
        withContext(Dispatchers.IO) {
            _tasteMemory.value = UserTasteMemory()
            context.tasteDataStore.edit { it.remove(TASTE_KEY) }
            Log.i(TAG, "Taste memory reset")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun ema(current: Float, signal: Float): Float =
        (current * (1f - ALPHA) + signal * ALPHA).coerceIn(-1f, 1f)

    companion object {
        private const val TAG = "TasteMemory"

        /** EMA learning rate — ~8 signals to reach steady state. */
        const val ALPHA = 0.25f

        /** Only include scores with |value| ≥ this in the LLM context string. */
        const val DISPLAY_THRESHOLD = 0.20f

        /** All valid genre tags from the LLM prompt vocabulary. */
        val KNOWN_GENRES = setOf(
            "pop", "jazz", "rock", "electronic", "ambient", "classical",
            "funk", "r&b", "hip-hop", "folk", "new-age", "blues",
        )

        /**
         * Pure function: builds a taste context string from [memory].
         * Extracted here so it can be unit-tested without Android dependencies.
         */
        fun buildTasteContextFrom(memory: UserTasteMemory): String {
            if (memory.feedbackCount < TasteMemoryRepository.MIN_FEEDBACK_FOR_CONTEXT) return ""

            val sb = StringBuilder()
            sb.append("Listener taste memory (${memory.feedbackCount} signals learned):\n")

            val preferredGenres = memory.genreScores.entries
                .filter { it.value >= DISPLAY_THRESHOLD }
                .sortedByDescending { it.value }
                .take(4)
            val avoidedGenres = memory.genreScores.entries
                .filter { it.value <= -DISPLAY_THRESHOLD }
                .sortedBy { it.value }
                .take(3)

            if (preferredGenres.isNotEmpty()) {
                sb.append("  Preferred genres : ${preferredGenres.joinToString { "${it.key} (${fmtScore(it.value)})" }}\n")
            }
            if (avoidedGenres.isNotEmpty()) {
                sb.append("  Avoid genres     : ${avoidedGenres.joinToString { "${it.key} (${fmtScore(it.value)})" }}\n")
            }

            val nonGenreTags = memory.tagScores.entries.filter { it.key !in KNOWN_GENRES }
            val preferredTags = nonGenreTags
                .filter { it.value >= DISPLAY_THRESHOLD }
                .sortedByDescending { it.value }
                .take(5)
            val avoidedTags = nonGenreTags
                .filter { it.value <= -DISPLAY_THRESHOLD }
                .sortedBy { it.value }
                .take(3)

            if (preferredTags.isNotEmpty()) {
                sb.append("  Preferred tags   : ${preferredTags.joinToString { "${it.key} (${fmtScore(it.value)})" }}\n")
            }
            if (avoidedTags.isNotEmpty()) {
                sb.append("  Avoid tags       : ${avoidedTags.joinToString { "${it.key} (${fmtScore(it.value)})" }}\n")
            }

            val contextEntries = memory.contextGenreScores.entries
                .filter { abs(it.value) >= DISPLAY_THRESHOLD }
                .sortedByDescending { it.value }
                .take(4)
            if (contextEntries.isNotEmpty()) {
                sb.append("  Scene context    : ${contextEntries.joinToString { "${it.key} (${fmtScore(it.value)})" }}\n")
            }

            sb.append("  Honour these unless overridden by stress ≥ 7 or SpO2 rules.")
            return sb.toString().trim()
        }

        private fun fmtScore(v: Float) = if (v >= 0f) "+${"%.2f".format(v)}" else "${"%.2f".format(v)}"
    }
}
