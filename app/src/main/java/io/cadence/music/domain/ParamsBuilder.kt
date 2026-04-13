package io.cadence.music.domain

import io.cadence.music.data.api.SongParams
import io.cadence.music.data.model.Scene
import io.cadence.music.data.model.SensorState

interface ParamsBuilder {
    suspend fun buildParams(state: SensorState, scene: Scene?): SongParams
}
