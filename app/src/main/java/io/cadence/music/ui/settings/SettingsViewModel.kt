package io.cadence.music.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cadence.music.data.settings.ApiSettings
import io.cadence.music.data.settings.ApiSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ApiSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<ApiSettings> = repo.settings
    val defaults: ApiSettings = repo.defaults

    fun save(
        signal2StyleBaseUrl: String,
        signal2StyleApiKey: String,
        signal2StyleModel: String,
        songGenBaseUrl: String,
        songGenApiKey: String,
        songGenModel: String,
        onResult: (Result) -> Unit,
    ) {
        val s2sUrlError = validateUrl(signal2StyleBaseUrl, "Signal2Style base URL")
        val sgUrlError  = validateUrl(songGenBaseUrl,      "SongGen base URL")
        val error = s2sUrlError ?: sgUrlError
        if (error != null) {
            onResult(Result.Invalid(error))
            return
        }
        viewModelScope.launch {
            repo.save(
                ApiSettings(
                    signal2StyleBaseUrl = signal2StyleBaseUrl.trim().trimEnd('/'),
                    signal2StyleApiKey  = signal2StyleApiKey.trim(),
                    signal2StyleModel   = signal2StyleModel.trim(),
                    songGenBaseUrl      = songGenBaseUrl.trim().trimEnd('/'),
                    songGenApiKey       = songGenApiKey.trim(),
                    songGenModel        = songGenModel.trim(),
                )
            )
            onResult(Result.Saved)
        }
    }

    fun resetAll(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.resetAll()
            onDone()
        }
    }

    private fun validateUrl(value: String, label: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "$label cannot be empty"
        return try {
            val url = URL(trimmed)
            if (url.protocol !in listOf("http", "https")) "$label must start with http:// or https://"
            else null
        } catch (_: MalformedURLException) {
            "$label is not a valid URL"
        }
    }

    sealed interface Result {
        data object Saved : Result
        data class Invalid(val message: String) : Result
    }
}
