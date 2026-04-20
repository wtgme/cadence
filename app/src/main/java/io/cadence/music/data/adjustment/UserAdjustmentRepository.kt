package io.cadence.music.data.adjustment

import io.cadence.music.data.model.UserMusicAdjustment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAdjustmentRepository @Inject constructor() {
    private val _adjustment = MutableStateFlow(UserMusicAdjustment())
    val adjustment: StateFlow<UserMusicAdjustment> = _adjustment

    fun toggleGenre(genre: String) = _adjustment.update { adj ->
        val current = adj.genreOverrides
        adj.copy(
            genreOverrides = if (genre in current) current - genre else current + genre
        )
    }

    fun clearGenres() = _adjustment.update { it.copy(genreOverrides = emptyList()) }

    fun setEnergyBias(delta: Int) = _adjustment.update { it.copy(energyBias = delta.coerceIn(-2, 2)) }

    fun setFreeText(text: String?) =
        _adjustment.update { it.copy(freeText = text?.takeIf { t -> t.isNotBlank() }) }

    fun reset() { _adjustment.value = UserMusicAdjustment() }
}
