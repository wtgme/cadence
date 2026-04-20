package io.cadence.music.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.cadence.music.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiSettingsDataStore by preferencesDataStore(name = "api_settings")

private val KEY_S2S_BASE_URL = stringPreferencesKey("signal2style_base_url")
private val KEY_S2S_API_KEY  = stringPreferencesKey("signal2style_api_key")
private val KEY_S2S_MODEL    = stringPreferencesKey("signal2style_model")
private val KEY_SG_BASE_URL  = stringPreferencesKey("songgen_base_url")
private val KEY_SG_API_KEY   = stringPreferencesKey("songgen_api_key")
private val KEY_SG_MODEL     = stringPreferencesKey("songgen_model")

/**
 * Persists user-configurable API endpoints. Defaults come from BuildConfig (local.properties).
 * Users can override any field via the Settings screen; `resetAll` restores defaults.
 */
@Singleton
class ApiSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val defaults = ApiSettings(
        signal2StyleBaseUrl = BuildConfig.SIGNAL2STYLE_BASE_URL,
        signal2StyleApiKey  = BuildConfig.SIGNAL2STYLE_API_KEY,
        signal2StyleModel   = BuildConfig.SIGNAL2STYLE_MODEL,
        songGenBaseUrl      = BuildConfig.SONGGEN_BASE_URL,
        songGenApiKey       = BuildConfig.SONGGEN_API_KEY,
        songGenModel        = BuildConfig.SONGGEN_MODEL,
    )

    private val _settings = MutableStateFlow(defaults)
    val settings: StateFlow<ApiSettings> = _settings

    val current: ApiSettings get() = _settings.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val prefs = context.apiSettingsDataStore.data.firstOrNull() ?: return
        _settings.value = ApiSettings(
            signal2StyleBaseUrl = prefs[KEY_S2S_BASE_URL] ?: defaults.signal2StyleBaseUrl,
            signal2StyleApiKey  = prefs[KEY_S2S_API_KEY]  ?: defaults.signal2StyleApiKey,
            signal2StyleModel   = prefs[KEY_S2S_MODEL]    ?: defaults.signal2StyleModel,
            songGenBaseUrl      = prefs[KEY_SG_BASE_URL]  ?: defaults.songGenBaseUrl,
            songGenApiKey       = prefs[KEY_SG_API_KEY]   ?: defaults.songGenApiKey,
            songGenModel        = prefs[KEY_SG_MODEL]     ?: defaults.songGenModel,
        )
        Log.d(TAG, "Loaded API settings (overrides applied)")
    }

    suspend fun save(settings: ApiSettings) = withContext(Dispatchers.IO) {
        context.apiSettingsDataStore.edit { prefs ->
            prefs[KEY_S2S_BASE_URL] = settings.signal2StyleBaseUrl
            prefs[KEY_S2S_API_KEY]  = settings.signal2StyleApiKey
            prefs[KEY_S2S_MODEL]    = settings.signal2StyleModel
            prefs[KEY_SG_BASE_URL]  = settings.songGenBaseUrl
            prefs[KEY_SG_API_KEY]   = settings.songGenApiKey
            prefs[KEY_SG_MODEL]     = settings.songGenModel
        }
        _settings.value = settings
        Log.i(TAG, "API settings saved")
    }

    suspend fun resetAll() = withContext(Dispatchers.IO) {
        context.apiSettingsDataStore.edit { it.clear() }
        _settings.value = defaults
        Log.i(TAG, "API settings reset to defaults")
    }

    companion object {
        private const val TAG = "ApiSettings"
    }
}
