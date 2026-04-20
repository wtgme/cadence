package io.cadence.music.data.model

data class UserMusicAdjustment(
    val genreOverrides: List<String> = emptyList(),
    val energyBias: Int = 0,
    val freeText: String? = null,
) {
    fun isEmpty() = genreOverrides.isEmpty() && energyBias == 0 && freeText == null

    fun toPromptHint(): String? {
        if (isEmpty()) return null
        val parts = buildList {
            if (genreOverrides.isNotEmpty()) add("Genre: ${genreOverrides.joinToString(", ")}")
            if (energyBias != 0) {
                val label = when {
                    energyBias >= 2  -> "much more energetic"
                    energyBias == 1  -> "more energetic"
                    energyBias == -1 -> "calmer"
                    else             -> "much calmer"
                }
                add("Energy: $label than default")
            }
            if (freeText != null) add(freeText)
        }
        return "User preference: ${parts.joinToString("; ")}. " +
            "Honour this unless it conflicts with the stress \u2265 7 or SpO2 safety rules."
    }
}
