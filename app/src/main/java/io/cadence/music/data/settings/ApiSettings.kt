package io.cadence.music.data.settings

data class ApiSettings(
    val signal2StyleBaseUrl: String,
    val signal2StyleApiKey: String,
    val signal2StyleModel: String,
    val songGenBaseUrl: String,
    val songGenApiKey: String,
    val songGenModel: String,
)
