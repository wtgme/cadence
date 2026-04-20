package io.cadence.music.data.session

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.MentalState
import io.cadence.music.data.model.Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val Context.lastSessionDataStore by preferencesDataStore(name = "last_session_params")

private val KEY_PARAMS_JSON       = stringPreferencesKey("params_json")
private val KEY_MENTAL_STATE_JSON = stringPreferencesKey("mental_state_json")
private val KEY_SCENE_NAME        = stringPreferencesKey("scene")
private val KEY_HEART_RATE        = longPreferencesKey("heart_rate")
private val KEY_SAVED_AT_MS       = longPreferencesKey("saved_at_ms")

@JsonClass(generateAdapter = true)
data class StoredMentalState(
    val arousal: Int?,
    val valence: Int?,
    val stress: Int?,
    val energy: Int?,
    val focus: Int?,
    val mood: String?,
)

/**
 * Snapshot of the last-established session style so a quick app re-open can skip Step 1
 * entirely. Considered "fresh" if saved within [FRESH_TTL_MS] and the user hasn't moved
 * meaningfully away in scene or HR.
 */
data class CachedSessionParams(
    val params: SongParams,
    val mentalState: MentalState?,
    val scene: Scene?,
    val heartRate: Int,
    val savedAtMs: Long,
)

/**
 * Abstraction over the persisted last-session cache so unit tests can substitute a no-op.
 */
interface LastSessionParamsStore {
    suspend fun load(): CachedSessionParams?
    suspend fun save(params: SongParams, mentalState: MentalState?, scene: Scene?, heartRate: Int)
    fun isFreshFor(cached: CachedSessionParams, currentScene: Scene?, currentHr: Int): Boolean
}

/**
 * Persists the most recent [SongParams] + [MentalState] so a short-interval relaunch can
 * reuse them instead of paying another Step 1 LLM round-trip. Only the fields actually
 * consumed by the generation pipeline are stored; [MentalState.rawLlmText] is dropped.
 */
@Singleton
class LastSessionParamsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
) : LastSessionParamsStore {
    private val paramsAdapter = moshi.adapter(SongParams::class.java)
    private val mentalStateAdapter = moshi.adapter(StoredMentalState::class.java)

    override suspend fun load(): CachedSessionParams? = withContext(Dispatchers.IO) {
        try {
            val prefs = context.lastSessionDataStore.data.firstOrNull() ?: return@withContext null
            val paramsJson = prefs[KEY_PARAMS_JSON] ?: return@withContext null
            val params = paramsAdapter.fromJson(paramsJson) ?: return@withContext null
            val mentalStateJson = prefs[KEY_MENTAL_STATE_JSON]
            val mental = mentalStateJson?.let { runCatching { mentalStateAdapter.fromJson(it) }.getOrNull() }
            val sceneName = prefs[KEY_SCENE_NAME]
            val scene = sceneName?.let { runCatching { Scene.valueOf(it) }.getOrNull() }
            val hr = prefs[KEY_HEART_RATE]?.toInt() ?: 0
            val savedAt = prefs[KEY_SAVED_AT_MS] ?: 0L
            CachedSessionParams(
                params = params,
                mentalState = mental?.let {
                    MentalState(
                        arousal = it.arousal,
                        valence = it.valence,
                        stress = it.stress,
                        energy = it.energy,
                        focus = it.focus,
                        mood = it.mood,
                        rawLlmText = "",
                    )
                },
                scene = scene,
                heartRate = hr,
                savedAtMs = savedAt,
            )
        } catch (e: Exception) {
            Log.w(TAG, "load failed: ${e.message}")
            null
        }
    }

    override suspend fun save(
        params: SongParams,
        mentalState: MentalState?,
        scene: Scene?,
        heartRate: Int,
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val paramsJson = paramsAdapter.toJson(params)
            val mentalJson = mentalState?.let {
                mentalStateAdapter.toJson(
                    StoredMentalState(
                        arousal = it.arousal,
                        valence = it.valence,
                        stress = it.stress,
                        energy = it.energy,
                        focus = it.focus,
                        mood = it.mood,
                    )
                )
            }
            context.lastSessionDataStore.edit { prefs ->
                prefs[KEY_PARAMS_JSON] = paramsJson
                if (mentalJson != null) prefs[KEY_MENTAL_STATE_JSON] = mentalJson
                else prefs.remove(KEY_MENTAL_STATE_JSON)
                if (scene != null) prefs[KEY_SCENE_NAME] = scene.name
                else prefs.remove(KEY_SCENE_NAME)
                prefs[KEY_HEART_RATE] = heartRate.toLong()
                prefs[KEY_SAVED_AT_MS] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.w(TAG, "save failed: ${e.message}")
        }
    }

    /**
     * `true` when [cached] is still representative of the user's current context:
     *   - saved within [FRESH_TTL_MS]
     *   - scene matches (or both unknown)
     *   - HR within [HR_BUCKET_BPM] of saved value (or HR missing on either side)
     */
    override fun isFreshFor(cached: CachedSessionParams, currentScene: Scene?, currentHr: Int): Boolean {
        val age = System.currentTimeMillis() - cached.savedAtMs
        if (age > FRESH_TTL_MS) return false
        if (cached.scene != currentScene) return false
        if (currentHr > 0 && cached.heartRate > 0 &&
            abs(currentHr - cached.heartRate) >= HR_BUCKET_BPM) return false
        return true
    }

    private companion object {
        const val TAG = "LastSessionParams"
        const val FRESH_TTL_MS = 10 * 60 * 1000L   // 10 min
        const val HR_BUCKET_BPM = 15
    }
}
